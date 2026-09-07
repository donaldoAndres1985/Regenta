package com.regenta.inventario.aplicacion;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.eventos.RegistroDeEventos;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.inventario.domain.Existencia;
import com.regenta.inventario.domain.MovimientoInventario;
import com.regenta.inventario.infra.ExistenciaRepositorio;
import com.regenta.inventario.infra.MovimientoInventarioRepositorio;

/**
 * El libro mayor de inventario. HU-030.
 *
 * <p>Todo cambio de stock pasa por aqui: se anota un movimiento append-only y,
 * en la MISMA transaccion, se ajusta la proyeccion {@code existencias}. Sumar
 * el libro de un producto en una bodega da siempre {@code existencias.cantidad}
 * (criterio 4). El mismo evento entregado dos veces se detecta por la
 * {@code idempotency_key} y no descuenta dos veces (criterio 3). Un error se
 * corrige con un movimiento contrario, nunca editando el original: lo hace
 * cumplir un trigger de la base (criterio 2 y 5).
 */
@Service
public class LibroMayorDeInventario {

    private final MovimientoInventarioRepositorio movimientos;
    private final ExistenciaRepositorio existencias;
    private final RegistroDeEventos eventos;

    public LibroMayorDeInventario(MovimientoInventarioRepositorio movimientos,
            ExistenciaRepositorio existencias, RegistroDeEventos eventos) {
        this.movimientos = movimientos;
        this.existencias = existencias;
        this.eventos = eventos;
    }

    @Transactional
    public RegistroDeMovimiento registrar(SolicitudDeMovimiento solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();

        var yaHecho = movimientos.findFirstByNegocioIdAndIdempotencyKey(
                negocioId, solicitud.idempotencyKey());
        if (yaHecho.isPresent()) {
            return comoDto(yaHecho.get(), true);
        }

        int signo = solicitud.tipo().signo();
        Existencia existencia = existencias
                .findByProductoIdAndBodegaId(solicitud.productoId(), solicitud.bodegaId())
                .orElseGet(() -> Existencia.enCero(negocioId, solicitud.productoId(),
                        solicitud.bodegaId()));

        BigDecimal saldoPosterior = existencia.saldoSiAplico(signo, solicitud.cantidad());
        existencia.aplicar(signo, solicitud.cantidad());
        existencias.save(existencia);

        MovimientoInventario movimiento = MovimientoInventario.de(negocioId,
                solicitud.productoId(), solicitud.bodegaId(), solicitud.tipo(),
                solicitud.cantidad(), saldoPosterior, solicitud.origenTipo(), solicitud.origenId(),
                ContextoDeNegocio.actual().usuario(), solicitud.motivo(),
                solicitud.idempotencyKey(), OffsetDateTime.now());
        movimientos.saveAndFlush(movimiento);

        eventos.registrar(negocioId, "existencia", solicitud.productoId(), "stock_actualizado",
                payload(negocioId, movimiento, saldoPosterior));
        return comoDto(movimiento, false);
    }

    /** Reconstruye el saldo sumando el libro. Debe coincidir con la proyeccion. */
    @Transactional(readOnly = true)
    public BigDecimal saldoSegunElLibro(UUID productoId, UUID bodegaId) {
        return movimientos.findByProductoIdAndBodegaIdOrderByOcurridoEnAsc(productoId, bodegaId)
                .stream()
                .map(MovimientoInventario::aporteConSigno)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public List<RegistroDeMovimiento> libroDelProducto(UUID productoId) {
        return movimientos.findByProductoIdOrderByOcurridoEnAsc(productoId).stream()
                .map(m -> comoDto(m, false))
                .toList();
    }

    private static Map<String, Object> payload(UUID negocioId, MovimientoInventario m,
            BigDecimal saldo) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("negocio_id", negocioId.toString());
        datos.put("producto_id", m.getProductoId().toString());
        datos.put("bodega_id", m.getBodegaId().toString());
        datos.put("tipo", m.getTipo().name());
        datos.put("cantidad", m.getCantidad());
        datos.put("saldo_posterior", saldo);
        return datos;
    }

    private static RegistroDeMovimiento comoDto(MovimientoInventario m, boolean duplicado) {
        return new RegistroDeMovimiento(m.getId(), m.getProductoId(), m.getBodegaId(), m.getTipo(),
                m.getSigno(), m.getCantidad(), m.getSaldoPosterior(), m.getOrigenTipo(),
                m.getOcurridoEn(), duplicado);
    }
}
