package com.regenta.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.inventario.domain.Bodega;
import com.regenta.inventario.domain.Lote;
import com.regenta.inventario.domain.OrigenMovimiento;
import com.regenta.inventario.domain.Producto;
import com.regenta.inventario.domain.TipoMovimiento;
import com.regenta.inventario.domain.Traslado;
import com.regenta.inventario.domain.TrasladoLinea;
import com.regenta.inventario.infra.BodegaRepositorio;
import com.regenta.inventario.infra.LoteRepositorio;
import com.regenta.inventario.infra.ProductoRepositorio;
import com.regenta.inventario.infra.TrasladoLineaRepositorio;
import com.regenta.inventario.infra.TrasladoRepositorio;

/**
 * Traslados entre bodegas. HU-032.
 *
 * <p>El ciclo es BORRADOR → EN_TRANSITO → RECIBIDO, y todo cambio de stock pasa
 * por {@link LibroMayorDeInventario}. Al enviar, la mercancia sale de la bodega
 * de origen y entra en una bodega de transito del negocio: mientras viaja sigue
 * contando en el inventario, no desaparece (criterio 1). Al recibir, sale de
 * transito y entra en destino (criterio 2). No se puede recibir mas de lo
 * enviado (criterio 3) ni trasladar de una bodega a si misma (criterio 4).
 */
@Service
public class GestionDeTraslados {

    private final TrasladoRepositorio traslados;
    private final TrasladoLineaRepositorio lineas;
    private final LibroMayorDeInventario libro;
    private final GestionDeBodegas bodegas;
    private final BodegaRepositorio bodegaRepo;
    private final ProductoRepositorio productos;
    private final LoteRepositorio lotes;

    public GestionDeTraslados(TrasladoRepositorio traslados, TrasladoLineaRepositorio lineas,
            LibroMayorDeInventario libro, GestionDeBodegas bodegas, BodegaRepositorio bodegaRepo,
            ProductoRepositorio productos, LoteRepositorio lotes) {
        this.traslados = traslados;
        this.lineas = lineas;
        this.libro = libro;
        this.bodegas = bodegas;
        this.bodegaRepo = bodegaRepo;
        this.productos = productos;
        this.lotes = lotes;
    }

    @Transactional
    @RequierePermiso("INVENTARIO_TRASLADO_CREAR")
    public TrasladoDelNegocio crear(SolicitudDeTraslado solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        bodegaDelNegocio(solicitud.bodegaOrigenId());
        bodegaDelNegocio(solicitud.bodegaDestinoId());
        if (traslados.existsByNegocioIdAndNumero(negocioId, solicitud.numero())) {
            throw new RecursoDuplicadoException(
                    "Ya hay un traslado con el numero " + solicitud.numero());
        }

        Traslado traslado = Traslado.crear(negocioId, solicitud.numero(),
                solicitud.bodegaOrigenId(), solicitud.bodegaDestinoId(), solicitud.observaciones());
        traslados.save(traslado);

        for (LineaDeTraslado linea : solicitud.lineas()) {
            Producto producto = productoDelNegocio(linea.productoId());
            UUID loteId = resolverLote(negocioId, producto, linea.codigoLote());
            lineas.save(TrasladoLinea.de(negocioId, traslado.getId(), producto.getId(), loteId,
                    linea.cantidad()));
        }
        return comoDto(traslado);
    }

    /** Criterio 1: BORRADOR → EN_TRANSITO. Sale de origen, entra a transito. */
    @Transactional
    @RequierePermiso("INVENTARIO_TRASLADO_ENVIAR")
    public TrasladoDelNegocio enviar(UUID trasladoId) {
        Traslado traslado = trasladoDelNegocio(trasladoId);
        traslado.enviar(ContextoDeNegocio.actual().usuario());

        UUID transito = bodegas.asegurarBodegaDeTransito().getId();
        for (TrasladoLinea linea : lineas.findByTrasladoIdOrderById(trasladoId)) {
            String codigoLote = codigoDeLote(linea);
            mover(linea, traslado.getBodegaOrigenId(), TipoMovimiento.SALIDA_TRASLADO,
                    linea.getCantidadEnviada(), traslado, "envio-salida", codigoLote);
            mover(linea, transito, TipoMovimiento.ENTRADA_TRASLADO,
                    linea.getCantidadEnviada(), traslado, "envio-entrada", codigoLote);
        }
        traslados.save(traslado);
        return comoDto(traslado);
    }

    /** Criterio 2: EN_TRANSITO → RECIBIDO. Sale de transito, entra a destino. */
    @Transactional
    @RequierePermiso("INVENTARIO_TRASLADO_RECIBIR")
    public TrasladoDelNegocio recibir(UUID trasladoId, SolicitudDeRecepcion solicitud) {
        Traslado traslado = trasladoDelNegocio(trasladoId);

        Map<UUID, BigDecimal> recibido = new HashMap<>();
        if (solicitud != null && solicitud.lineas() != null) {
            for (LineaRecibida lr : solicitud.lineas()) {
                recibido.put(lr.lineaId(), lr.cantidadRecibida());
            }
        }

        traslado.recibir(ContextoDeNegocio.actual().usuario());

        UUID transito = bodegas.asegurarBodegaDeTransito().getId();
        for (TrasladoLinea linea : lineas.findByTrasladoIdOrderById(trasladoId)) {
            BigDecimal cantidad = recibido.getOrDefault(linea.getId(), linea.pendiente());
            if (cantidad.signum() < 0) {
                throw new ReglaDeNegocioException("La cantidad recibida no puede ser negativa");
            }
            if (cantidad.signum() == 0) {
                continue;
            }
            linea.registrarRecepcion(cantidad);   // criterio 3: no mas de lo enviado
            lineas.save(linea);

            String codigoLote = codigoDeLote(linea);
            mover(linea, transito, TipoMovimiento.SALIDA_TRASLADO, cantidad, traslado,
                    "recepcion-salida", codigoLote);
            mover(linea, traslado.getBodegaDestinoId(), TipoMovimiento.ENTRADA_TRASLADO, cantidad,
                    traslado, "recepcion-entrada", codigoLote);
        }
        traslados.save(traslado);
        return comoDto(traslado);
    }

    @Transactional(readOnly = true)
    @RequierePermiso("INVENTARIO_TRASLADO_VER")
    public TrasladoDelNegocio ver(UUID trasladoId) {
        return comoDto(trasladoDelNegocio(trasladoId));
    }

    private void mover(TrasladoLinea linea, UUID bodegaId, TipoMovimiento tipo, BigDecimal cantidad,
            Traslado traslado, String fase, String codigoLote) {
        libro.registrar(new SolicitudDeMovimiento(linea.getProductoId(), bodegaId, tipo, cantidad,
                OrigenMovimiento.TRASLADO, traslado.getId(),
                "Traslado " + traslado.getNumero() + " (" + fase + ")",
                "traslado:" + traslado.getId() + ":linea:" + linea.getId() + ":" + fase,
                codigoLote, null));
    }

    private UUID resolverLote(UUID negocioId, Producto producto, String codigo) {
        if (!producto.isManejaLotes()) {
            return null;
        }
        String cod = codigo == null ? "" : codigo.trim();
        if (cod.isBlank()) {
            throw new ReglaDeNegocioException("El producto \"" + producto.getNombre()
                    + "\" maneja lotes: indica el codigo de lote de la linea");
        }
        return lotes.findByNegocioIdAndProductoIdAndCodigoLote(negocioId, producto.getId(), cod)
                .orElseThrow(() -> new NoEncontradoException(
                        "El lote " + cod + " no existe para ese producto"))
                .getId();
    }

    private String codigoDeLote(TrasladoLinea linea) {
        if (linea.getLoteId() == null) {
            return null;
        }
        return lotes.findById(linea.getLoteId()).map(Lote::getCodigoLote).orElse(null);
    }

    private Bodega bodegaDelNegocio(UUID bodegaId) {
        Bodega bodega = bodegaRepo.findById(bodegaId)
                .orElseThrow(() -> new NoEncontradoException("Esa bodega no existe"));
        if (!bodega.getNegocioId().equals(ContextoDeNegocio.negocioActual())) {
            throw new NoEncontradoException("Esa bodega no existe");
        }
        return bodega;
    }

    private Producto productoDelNegocio(UUID productoId) {
        Producto producto = productos.findById(productoId)
                .orElseThrow(() -> new NoEncontradoException("Ese producto no existe"));
        if (!producto.getNegocioId().equals(ContextoDeNegocio.negocioActual())) {
            throw new NoEncontradoException("Ese producto no existe");
        }
        return producto;
    }

    private Traslado trasladoDelNegocio(UUID trasladoId) {
        Traslado traslado = traslados.findById(trasladoId)
                .orElseThrow(() -> new NoEncontradoException("Ese traslado no existe"));
        if (!traslado.getNegocioId().equals(ContextoDeNegocio.negocioActual())) {
            throw new NoEncontradoException("Ese traslado no existe");
        }
        return traslado;
    }

    private TrasladoDelNegocio comoDto(Traslado traslado) {
        List<LineaDelTraslado> dto = lineas.findByTrasladoIdOrderById(traslado.getId()).stream()
                .map(l -> new LineaDelTraslado(l.getId(), l.getProductoId(), l.getLoteId(),
                        l.getCantidadEnviada(), l.getCantidadRecibida()))
                .toList();
        return new TrasladoDelNegocio(traslado.getId(), traslado.getNumero(),
                traslado.getBodegaOrigenId(), traslado.getBodegaDestinoId(), traslado.getEstado(),
                traslado.getFechaEnvio(), traslado.getFechaRecepcion(), dto);
    }
}
