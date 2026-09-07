package com.regenta.inventario.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.inventario.domain.Bodega;
import com.regenta.inventario.domain.ExistenciaLote;
import com.regenta.inventario.domain.Lote;
import com.regenta.inventario.domain.OrigenMovimiento;
import com.regenta.inventario.domain.Producto;
import com.regenta.inventario.domain.TipoMovimiento;
import com.regenta.inventario.infra.BodegaRepositorio;
import com.regenta.inventario.infra.ExistenciaLoteRepositorio;
import com.regenta.inventario.infra.LoteRepositorio;
import com.regenta.inventario.infra.ProductoRepositorio;

/**
 * Lotes y fechas de vencimiento. HU-031.
 *
 * <p>Las reglas de stock (exigir el codigo de lote al entrar, bloquear el lote
 * vencido al salir) viven en {@link LibroMayorDeInventario}: se cumplen venga la
 * salida de aqui o de la saga de ventas. Este servicio pone encima el alta del
 * lote con sus fechas, la consulta de existencia por bodega y la sugerencia
 * FEFO.
 */
@Service
public class GestionDeLotes {

    private final LibroMayorDeInventario libro;
    private final ProductoRepositorio productos;
    private final LoteRepositorio lotes;
    private final ExistenciaLoteRepositorio existenciasLote;
    private final BodegaRepositorio bodegas;

    public GestionDeLotes(LibroMayorDeInventario libro, ProductoRepositorio productos,
            LoteRepositorio lotes, ExistenciaLoteRepositorio existenciasLote,
            BodegaRepositorio bodegas) {
        this.libro = libro;
        this.productos = productos;
        this.lotes = lotes;
        this.existenciasLote = existenciasLote;
        this.bodegas = bodegas;
    }

    /** Criterio 1: al entrar mercancia de un producto que maneja lotes, se exige el codigo. */
    @Transactional
    @RequierePermiso("INVENTARIO_LOTE_REGISTRAR")
    public RegistroDeLote registrarEntrada(SolicitudDeEntradaDeLote solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        Producto producto = productoDelNegocio(solicitud.productoId());
        exigirQueManejeLotes(producto);

        String codigo = solicitud.codigoLote() == null ? "" : solicitud.codigoLote().trim();
        if (codigo.isBlank()) {
            throw new ReglaDeNegocioException(
                    "Indica el codigo de lote de la mercancia que entra");
        }

        Lote lote = lotes
                .findByNegocioIdAndProductoIdAndCodigoLote(negocioId, producto.getId(), codigo)
                .orElseGet(() -> Lote.nuevo(negocioId, producto.getId(), codigo));
        lote.datosDeRecepcion(solicitud.fechaFabricacion(), solicitud.fechaVencimiento(),
                solicitud.registroSanitario(), solicitud.costoUnitario());
        lotes.save(lote);

        RegistroDeMovimiento movimiento = libro.registrar(new SolicitudDeMovimiento(
                producto.getId(), solicitud.bodegaId(), TipoMovimiento.ENTRADA_COMPRA,
                solicitud.cantidad(), OrigenMovimiento.RECEPCION, null, solicitud.motivo(),
                solicitud.idempotencyKey(), codigo, null));

        BigDecimal enBodega = existenciasLote
                .findByLoteIdAndBodegaId(lote.getId(), solicitud.bodegaId())
                .map(ExistenciaLote::getCantidad).orElse(BigDecimal.ZERO);
        return new RegistroDeLote(lote.getId(), lote.getCodigoLote(), lote.getFechaVencimiento(),
                lote.estaVencido(LocalDate.now()), enBodega, movimiento);
    }

    /**
     * Criterio 4: la salida de un lote vencido se bloquea salvo autorizacion
     * explicita, que queda registrada. El libro mayor es quien lo hace cumplir.
     */
    @Transactional
    @RequierePermiso("INVENTARIO_LOTE_REGISTRAR")
    public RegistroDeMovimiento registrarSalida(SolicitudDeSalidaDeLote solicitud) {
        Producto producto = productoDelNegocio(solicitud.productoId());
        exigirQueManejeLotes(producto);
        return libro.registrar(new SolicitudDeMovimiento(producto.getId(), solicitud.bodegaId(),
                TipoMovimiento.SALIDA_VENTA, solicitud.cantidad(), OrigenMovimiento.VENTA, null,
                solicitud.motivo(), solicitud.idempotencyKey(), solicitud.codigoLote(),
                solicitud.autorizacionVencido()));
    }

    /** Criterio 2: la existencia del lote, desglosada por bodega. */
    @Transactional(readOnly = true)
    @RequierePermiso("INVENTARIO_LOTE_VER")
    public ExistenciaDeLote existenciaDelLote(UUID loteId) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        Lote lote = lotes.findById(loteId)
                .filter(l -> l.getNegocioId().equals(negocioId))
                .orElseThrow(() -> new NoEncontradoException("Ese lote no existe"));

        Map<UUID, String> nombreDeBodega = bodegas.findByNegocioIdOrderByCodigo(negocioId).stream()
                .collect(Collectors.toMap(Bodega::getId, Bodega::getNombre));

        List<ExistenciaLoteEnBodega> lineas = existenciasLote.findByLoteIdOrderByBodegaId(loteId)
                .stream()
                .map(e -> new ExistenciaLoteEnBodega(e.getBodegaId(),
                        nombreDeBodega.getOrDefault(e.getBodegaId(), "?"), e.getCantidad()))
                .toList();
        BigDecimal total = lineas.stream().map(ExistenciaLoteEnBodega::cantidad)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ExistenciaDeLote(lote.getId(), lote.getCodigoLote(), lote.getFechaVencimiento(),
                lote.estaVencido(LocalDate.now()), lineas, total);
    }

    /**
     * Criterio 3: para una salida, sugiere de que lotes tomar, del que vence
     * antes al que vence despues. El lote vencido queda fuera.
     */
    @Transactional(readOnly = true)
    @RequierePermiso("INVENTARIO_LOTE_VER")
    public SugerenciaFefo sugerirFefo(UUID productoId, UUID bodegaId, BigDecimal cantidad) {
        productoDelNegocio(productoId);

        List<AsignacionFefo> asignaciones = new ArrayList<>();
        BigDecimal restante = cantidad;
        for (FilaFefo fila : existenciasLote.fefo(productoId, bodegaId, LocalDate.now())) {
            if (restante.signum() <= 0) {
                break;
            }
            BigDecimal toma = fila.disponible().min(restante);
            asignaciones.add(new AsignacionFefo(fila.loteId(), fila.codigoLote(),
                    fila.fechaVencimiento(), fila.disponible(), toma));
            restante = restante.subtract(toma);
        }
        return new SugerenciaFefo(productoId, bodegaId, cantidad, restante.signum() <= 0,
                asignaciones);
    }

    private Producto productoDelNegocio(UUID productoId) {
        Producto producto = productos.findById(productoId)
                .orElseThrow(() -> new NoEncontradoException("Ese producto no existe"));
        if (!producto.getNegocioId().equals(ContextoDeNegocio.negocioActual())) {
            throw new NoEncontradoException("Ese producto no existe");
        }
        return producto;
    }

    private static void exigirQueManejeLotes(Producto producto) {
        if (!producto.isManejaLotes()) {
            throw new ReglaDeNegocioException("El producto \"" + producto.getNombre()
                    + "\" no maneja lotes");
        }
    }
}
