package com.regenta.inventario.application;

import com.regenta.inventario.api.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.security.RequierePermiso;
import com.regenta.inventario.domain.Existencia;
import com.regenta.inventario.domain.OrigenMovimiento;
import com.regenta.inventario.domain.ReservaStock;
import com.regenta.inventario.domain.TipoMovimiento;
import com.regenta.inventario.infrastructure.ExistenciaRepositorio;
import com.regenta.inventario.infrastructure.ReservaStockRepositorio;

/**
 * Reserva y liberaciÃ³n de stock para la saga de ventas. HU-034.
 *
 * <p>{@code solicitar} aparta stock: bloquea la fila de existencia, comprueba lo
 * disponible y sube {@code cantidad_reservada}, o publica
 * {@code stock_reserva_fallida} si no alcanza. El bloqueo es lo que impide que
 * dos cajas vendan la Ãºltima unidad (criterio 4). {@code confirmar} â€”lo dispara
 * {@code venta_completada}â€” convierte la reserva en salida real.
 * {@code liberarExpiradas} es el barrido que suelta lo que quedÃ³ apartado sin
 * confirmarse: sin Ã©l, una venta abandonada bloquea stock para siempre.
 */
@Service
public class GestionDeReservasStock {

    // DIAGNOSTICO TEMPORAL: confirmar si la respuesta de la saga (stock_reservado
    // / stock_reserva_fallida) de verdad sale publicada. Quitar una vez resuelto.
    private static final Logger DIAG = LoggerFactory.getLogger("DIAG_SAGA");

    /** TTL por defecto de una reserva si la solicitud no trae {@code expiraEn}. */
    static final Duration TTL_POR_DEFECTO = Duration.ofMinutes(15);

    private final ReservaStockRepositorio reservas;
    private final ExistenciaRepositorio existencias;
    private final LibroMayorDeInventario libro;
    private final RegistroDeEventos eventos;

    public GestionDeReservasStock(ReservaStockRepositorio reservas, ExistenciaRepositorio existencias,
            LibroMayorDeInventario libro, RegistroDeEventos eventos) {
        this.reservas = reservas;
        this.existencias = existencias;
        this.libro = libro;
        this.eventos = eventos;
    }

    /** Criterios 1, 2 y 4. */
    @Transactional
    @RequierePermiso("INVENTARIO_RESERVA_GESTIONAR")
    public ResultadoDeReserva solicitar(SolicitudDeReservaStock solicitud) {
        DIAG.info("solicitar() ENTRA origenId={} correlacion={}", solicitud.origenId(), solicitud.correlacionId());
        UUID negocioId = ContextoDeNegocio.negocioActual();

        List<ReservaStock> yaReservado = reservas.findByNegocioIdAndOrigenTipoAndOrigenId(
                negocioId, solicitud.origenTipo(), solicitud.origenId());
        if (!yaReservado.isEmpty()) {
            // Reintento/redelivery de una solicitud que ya se resolvio antes: hay que
            // responder igual (idempotente), o Ventas se queda esperando una respuesta
            // que nunca llega y la saga se traba en PENDIENTE_STOCK ("venta sin stock").
            List<UUID> ids = yaReservado.stream().map(ReservaStock::getId).toList();
            var evt = eventos.registrar(negocioId, "reserva_stock", solicitud.origenId(), "stock_reservado",
                    payloadReservado(negocioId, solicitud, ids));
            DIAG.info("publicado stock_reservado (idempotente, ya existia) id={} correlacion={} reservas={}",
                    evt.id(), solicitud.correlacionId(), ids.size());
            return new ResultadoDeReserva(true, solicitud.correlacionId(), List.of(), ids);
        }

        OffsetDateTime expira = solicitud.expiraEn() != null ? solicitud.expiraEn()
                : OffsetDateTime.now().plus(TTL_POR_DEFECTO);

        Map<LineaDeReserva, Existencia> aReservar = new LinkedHashMap<>();
        List<FaltanteDeStock> faltantes = new ArrayList<>();
        for (LineaDeReserva linea : solicitud.lineas()) {
            Existencia existencia = existencias
                    .bloquearPorProductoYBodega(linea.productoId(), linea.bodegaId())
                    .orElse(null);
            BigDecimal disponible = existencia == null ? BigDecimal.ZERO : existencia.disponible();
            if (disponible.compareTo(linea.cantidad()) < 0) {
                faltantes.add(new FaltanteDeStock(linea.productoId(), linea.bodegaId(),
                        linea.cantidad(), disponible));
            } else {
                aReservar.put(linea, existencia);
            }
        }

        if (!faltantes.isEmpty()) {
            var evt = eventos.registrar(negocioId, "reserva_stock", solicitud.origenId(),
                    "stock_reserva_fallida", payloadFallida(negocioId, solicitud, faltantes));
            DIAG.info("publicado stock_reserva_fallida id={} correlacion={} faltantes={}",
                    evt.id(), solicitud.correlacionId(), faltantes.size());
            return new ResultadoDeReserva(false, solicitud.correlacionId(), faltantes, List.of());
        }

        List<UUID> ids = new ArrayList<>();
        aReservar.forEach((linea, existencia) -> {
            existencia.reservar(linea.cantidad());
            existencias.save(existencia);
            ReservaStock reserva = ReservaStock.nueva(negocioId, linea.productoId(),
                    linea.bodegaId(), linea.cantidad(), solicitud.origenTipo(), solicitud.origenId(),
                    solicitud.correlacionId(), expira);
            reservas.save(reserva);
            ids.add(reserva.getId());
        });

        var evt = eventos.registrar(negocioId, "reserva_stock", solicitud.origenId(), "stock_reservado",
                payloadReservado(negocioId, solicitud, ids));
        DIAG.info("publicado stock_reservado id={} correlacion={} reservas={}",
                evt.id(), solicitud.correlacionId(), ids.size());
        return new ResultadoDeReserva(true, solicitud.correlacionId(), List.of(), ids);
    }

    /** Criterio 5: {@code venta_completada} convierte la reserva en salida real. */
    @Transactional
    @RequierePermiso("INVENTARIO_RESERVA_GESTIONAR")
    public void confirmar(String origenTipo, UUID origenId) {
        DIAG.info("confirmar() ENTRA origenTipo={} origenId={}", origenTipo, origenId);
        UUID negocioId = ContextoDeNegocio.negocioActual();
        List<ReservaStock> encontradas = reservas.findByNegocioIdAndOrigenTipoAndOrigenId(
                negocioId, origenTipo, origenId);
        DIAG.info("confirmar() encontro {} reservas para origenId={}", encontradas.size(), origenId);
        for (ReservaStock reserva : encontradas) {
            if (!reserva.estaActiva()) {
                continue;   // ya confirmada o liberada: idempotente
            }
            reserva.confirmar();
            existencias.bloquearPorProductoYBodega(reserva.getProductoId(), reserva.getBodegaId())
                    .ifPresent(existencia -> {
                        existencia.liberarReserva(reserva.getCantidad());
                        existencias.save(existencia);
                    });
            libro.registrar(new SolicitudDeMovimiento(reserva.getProductoId(), reserva.getBodegaId(),
                    TipoMovimiento.SALIDA_VENTA, reserva.getCantidad(), OrigenMovimiento.SAGA,
                    origenId, "Venta confirmada (saga " + reserva.getCorrelacionId() + ")",
                    "reserva:" + reserva.getId() + ":confirmar"));
        }
    }

    /**
     * Criterio 3: suelta las reservas activas que ya vencieron. Corre dentro del
     * negocio en contexto; el barrido multi-tenant programado se conecta con la
     * saga de E04.
     */
    @Transactional
    public int liberarExpiradas() {
        List<ReservaStock> vencidas = reservas.tomarVencidas(OffsetDateTime.now(),
                PageRequest.of(0, 200));
        for (ReservaStock reserva : vencidas) {
            reserva.expirar();
            existencias.bloquearPorProductoYBodega(reserva.getProductoId(), reserva.getBodegaId())
                    .ifPresent(existencia -> {
                        existencia.liberarReserva(reserva.getCantidad());
                        existencias.save(existencia);
                    });
        }
        return vencidas.size();
    }

    private static Map<String, Object> payloadReservado(UUID negocioId, SolicitudDeReservaStock s,
            List<UUID> ids) {
        Map<String, Object> datos = cabecera(negocioId, s);
        datos.put("reservas", ids.stream().map(UUID::toString).toList());
        return datos;
    }

    private static Map<String, Object> payloadFallida(UUID negocioId, SolicitudDeReservaStock s,
            List<FaltanteDeStock> faltantes) {
        Map<String, Object> datos = cabecera(negocioId, s);
        List<Map<String, Object>> detalle = new ArrayList<>();
        for (FaltanteDeStock f : faltantes) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("producto_id", f.productoId().toString());
            item.put("bodega_id", f.bodegaId().toString());
            item.put("solicitado", f.solicitado());
            item.put("disponible", f.disponible());
            detalle.add(item);
        }
        datos.put("faltantes", detalle);
        return datos;
    }

    private static Map<String, Object> cabecera(UUID negocioId, SolicitudDeReservaStock s) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("negocio_id", negocioId.toString());
        datos.put("origen_tipo", s.origenTipo());
        datos.put("origen_id", s.origenId().toString());
        datos.put("correlacion_id", s.correlacionId().toString());
        return datos;
    }
}
