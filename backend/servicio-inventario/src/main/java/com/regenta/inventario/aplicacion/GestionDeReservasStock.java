package com.regenta.inventario.aplicacion;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.eventos.RegistroDeEventos;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.inventario.domain.Existencia;
import com.regenta.inventario.domain.OrigenMovimiento;
import com.regenta.inventario.domain.ReservaStock;
import com.regenta.inventario.domain.TipoMovimiento;
import com.regenta.inventario.infra.ExistenciaRepositorio;
import com.regenta.inventario.infra.ReservaStockRepositorio;

/**
 * Reserva y liberación de stock para la saga de ventas. HU-034.
 *
 * <p>{@code solicitar} aparta stock: bloquea la fila de existencia, comprueba lo
 * disponible y sube {@code cantidad_reservada}, o publica
 * {@code stock_reserva_fallida} si no alcanza. El bloqueo es lo que impide que
 * dos cajas vendan la última unidad (criterio 4). {@code confirmar} —lo dispara
 * {@code venta_completada}— convierte la reserva en salida real.
 * {@code liberarExpiradas} es el barrido que suelta lo que quedó apartado sin
 * confirmarse: sin él, una venta abandonada bloquea stock para siempre.
 */
@Service
public class GestionDeReservasStock {

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
        UUID negocioId = ContextoDeNegocio.negocioActual();

        List<ReservaStock> yaReservado = reservas.findByNegocioIdAndOrigenTipoAndOrigenId(
                negocioId, solicitud.origenTipo(), solicitud.origenId());
        if (!yaReservado.isEmpty()) {
            return new ResultadoDeReserva(true, solicitud.correlacionId(), List.of(),
                    yaReservado.stream().map(ReservaStock::getId).toList());
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
            eventos.registrar(negocioId, "reserva_stock", solicitud.origenId(),
                    "stock_reserva_fallida", payloadFallida(negocioId, solicitud, faltantes));
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

        eventos.registrar(negocioId, "reserva_stock", solicitud.origenId(), "stock_reservado",
                payloadReservado(negocioId, solicitud, ids));
        return new ResultadoDeReserva(true, solicitud.correlacionId(), List.of(), ids);
    }

    /** Criterio 5: {@code venta_completada} convierte la reserva en salida real. */
    @Transactional
    @RequierePermiso("INVENTARIO_RESERVA_GESTIONAR")
    public void confirmar(String origenTipo, UUID origenId) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        for (ReservaStock reserva : reservas.findByNegocioIdAndOrigenTipoAndOrigenId(
                negocioId, origenTipo, origenId)) {
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
