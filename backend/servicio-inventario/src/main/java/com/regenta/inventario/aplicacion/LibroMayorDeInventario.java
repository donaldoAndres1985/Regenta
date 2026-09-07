package com.regenta.inventario.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.eventos.RegistroDeEventos;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.inventario.domain.AutorizacionLoteVencido;
import com.regenta.inventario.domain.Existencia;
import com.regenta.inventario.domain.ExistenciaLote;
import com.regenta.inventario.domain.Lote;
import com.regenta.inventario.domain.MovimientoInventario;
import com.regenta.inventario.domain.Producto;
import com.regenta.inventario.infra.AutorizacionLoteVencidoRepositorio;
import com.regenta.inventario.infra.ExistenciaLoteRepositorio;
import com.regenta.inventario.infra.ExistenciaRepositorio;
import com.regenta.inventario.infra.LoteRepositorio;
import com.regenta.inventario.infra.MovimientoInventarioRepositorio;
import com.regenta.inventario.infra.ProductoRepositorio;

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
 *
 * <p>HU-031: si el producto maneja lotes, el mismo paso ajusta
 * {@code existencias_lote}. Una entrada sin codigo de lote se rechaza; una
 * salida de un lote vencido, tambien, salvo que traiga autorizacion explicita,
 * y entonces la autorizacion queda registrada.
 */
@Service
public class LibroMayorDeInventario {

    private final MovimientoInventarioRepositorio movimientos;
    private final ExistenciaRepositorio existencias;
    private final ProductoRepositorio productos;
    private final LoteRepositorio lotes;
    private final ExistenciaLoteRepositorio existenciasLote;
    private final AutorizacionLoteVencidoRepositorio autorizaciones;
    private final RegistroDeEventos eventos;

    public LibroMayorDeInventario(MovimientoInventarioRepositorio movimientos,
            ExistenciaRepositorio existencias, ProductoRepositorio productos, LoteRepositorio lotes,
            ExistenciaLoteRepositorio existenciasLote,
            AutorizacionLoteVencidoRepositorio autorizaciones, RegistroDeEventos eventos) {
        this.movimientos = movimientos;
        this.existencias = existencias;
        this.productos = productos;
        this.lotes = lotes;
        this.existenciasLote = existenciasLote;
        this.autorizaciones = autorizaciones;
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
        UUID loteId = aplicarLoteSiCorresponde(negocioId, solicitud, signo);

        Existencia existencia = existencias
                .findByProductoIdAndBodegaId(solicitud.productoId(), solicitud.bodegaId())
                .orElseGet(() -> Existencia.enCero(negocioId, solicitud.productoId(),
                        solicitud.bodegaId()));

        BigDecimal saldoPosterior = existencia.saldoSiAplico(signo, solicitud.cantidad());
        existencia.aplicar(signo, solicitud.cantidad());
        existencias.save(existencia);

        MovimientoInventario movimiento = MovimientoInventario.de(negocioId,
                solicitud.productoId(), solicitud.bodegaId(), loteId, solicitud.tipo(),
                solicitud.cantidad(), saldoPosterior, solicitud.origenTipo(), solicitud.origenId(),
                ContextoDeNegocio.actual().usuario(), solicitud.motivo(),
                solicitud.idempotencyKey(), OffsetDateTime.now());
        movimientos.saveAndFlush(movimiento);

        eventos.registrar(negocioId, "existencia", solicitud.productoId(), "stock_actualizado",
                payload(negocioId, movimiento, saldoPosterior));
        return comoDto(movimiento, false);
    }

    /**
     * Ajusta {@code existencias_lote} cuando el producto maneja lotes y devuelve
     * el id del lote afectado. HU-031.
     */
    private UUID aplicarLoteSiCorresponde(UUID negocioId, SolicitudDeMovimiento s, int signo) {
        if (signo == 0) {
            return null;   // RESERVA / LIBERACION_RESERVA no mueven lotes
        }
        Producto producto = productos.findById(s.productoId())
                .filter(p -> p.getNegocioId().equals(negocioId))
                .orElseThrow(() -> new NoEncontradoException("Ese producto no existe"));
        if (!producto.isManejaLotes()) {
            return null;
        }

        boolean entrada = signo > 0;
        String codigo = s.codigoLote() == null ? "" : s.codigoLote().trim();
        if (codigo.isBlank()) {
            if (entrada) {
                throw new ReglaDeNegocioException("El producto \"" + producto.getNombre()
                        + "\" maneja lotes: indica el codigo de lote de la mercancia que entra");
            }
            return null;   // salida sin lote: el FEFO es sugerencia, no obligacion
        }

        Lote lote = lotes
                .findByNegocioIdAndProductoIdAndCodigoLote(negocioId, producto.getId(), codigo)
                .orElseGet(() -> {
                    if (!entrada) {
                        throw new NoEncontradoException(
                                "El lote " + codigo + " no existe para ese producto");
                    }
                    return lotes.save(Lote.nuevo(negocioId, producto.getId(), codigo));
                });

        ExistenciaLote existenciaLote = existenciasLote
                .findByLoteIdAndBodegaId(lote.getId(), s.bodegaId())
                .orElseGet(() -> ExistenciaLote.enCero(negocioId, lote.getId(), s.bodegaId()));

        if (!entrada) {
            if (lote.estaVencido(LocalDate.now())) {
                if (s.autorizacionVencido() == null || s.autorizacionVencido().isBlank()) {
                    throw new ReglaDeNegocioException("El lote " + codigo + " esta vencido (vencio el "
                            + lote.getFechaVencimiento()
                            + "): se requiere autorizacion explicita para darle salida");
                }
                autorizaciones.save(AutorizacionLoteVencido.de(negocioId, lote, s.bodegaId(),
                        s.cantidad(), ContextoDeNegocio.actual().usuario(),
                        s.autorizacionVencido().trim()));
            }
            if (existenciaLote.getCantidad().compareTo(s.cantidad()) < 0) {
                throw new ReglaDeNegocioException("No hay suficiente del lote " + codigo
                        + " en esa bodega: hay " + existenciaLote.getCantidad() + " y se piden "
                        + s.cantidad());
            }
        }

        existenciaLote.aplicar(signo, s.cantidad());
        existenciasLote.save(existenciaLote);
        return lote.getId();
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
