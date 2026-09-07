package com.regenta.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.inventario.domain.Ajuste;
import com.regenta.inventario.domain.AjusteLinea;
import com.regenta.inventario.domain.Bodega;
import com.regenta.inventario.domain.Existencia;
import com.regenta.inventario.domain.ExistenciaLote;
import com.regenta.inventario.domain.Lote;
import com.regenta.inventario.domain.OrigenMovimiento;
import com.regenta.inventario.domain.Producto;
import com.regenta.inventario.domain.TipoMovimiento;
import com.regenta.inventario.infra.AjusteLineaRepositorio;
import com.regenta.inventario.infra.AjusteRepositorio;
import com.regenta.inventario.infra.BodegaRepositorio;
import com.regenta.inventario.infra.ExistenciaLoteRepositorio;
import com.regenta.inventario.infra.ExistenciaRepositorio;
import com.regenta.inventario.infra.LoteRepositorio;
import com.regenta.inventario.infra.ProductoRepositorio;

/**
 * Ajustes de inventario con motivo. HU-033.
 *
 * <p>Se carga en BORRADOR: por cada línea, la cantidad de sistema se toma de la
 * proyección {@code existencias} en ese momento y la física la trae quien
 * cuenta; la diferencia la calcula la base (criterio 1). Al aplicar se generan
 * los movimientos de entrada o salida por la diferencia y el ajuste queda
 * cerrado (criterio 2); aplicar sin motivo se rechaza (criterio 3); y queda
 * constancia de quién lo cargó y quién lo aprobó (criterio 4).
 */
@Service
public class GestionDeAjustes {

    private final AjusteRepositorio ajustes;
    private final AjusteLineaRepositorio lineas;
    private final LibroMayorDeInventario libro;
    private final ExistenciaRepositorio existencias;
    private final ExistenciaLoteRepositorio existenciasLote;
    private final BodegaRepositorio bodegas;
    private final ProductoRepositorio productos;
    private final LoteRepositorio lotes;

    public GestionDeAjustes(AjusteRepositorio ajustes, AjusteLineaRepositorio lineas,
            LibroMayorDeInventario libro, ExistenciaRepositorio existencias,
            ExistenciaLoteRepositorio existenciasLote, BodegaRepositorio bodegas,
            ProductoRepositorio productos, LoteRepositorio lotes) {
        this.ajustes = ajustes;
        this.lineas = lineas;
        this.libro = libro;
        this.existencias = existencias;
        this.existenciasLote = existenciasLote;
        this.bodegas = bodegas;
        this.productos = productos;
        this.lotes = lotes;
    }

    @Transactional
    @RequierePermiso("INVENTARIO_AJUSTE_CREAR")
    public AjusteDelNegocio crear(SolicitudDeAjuste solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        bodegaDelNegocio(solicitud.bodegaId());
        if (ajustes.existsByNegocioIdAndNumero(negocioId, solicitud.numero())) {
            throw new RecursoDuplicadoException(
                    "Ya hay un ajuste con el numero " + solicitud.numero());
        }

        Ajuste ajuste = Ajuste.crear(negocioId, solicitud.numero(), solicitud.bodegaId(),
                solicitud.tipo(), solicitud.motivo(), ContextoDeNegocio.actual().usuario());
        ajustes.save(ajuste);

        for (LineaDeAjuste linea : solicitud.lineas()) {
            Producto producto = productoDelNegocio(linea.productoId());
            UUID loteId = resolverLote(negocioId, producto, linea.codigoLote());
            BigDecimal sistema = cantidadDeSistema(producto.getId(), solicitud.bodegaId(), loteId);
            lineas.save(AjusteLinea.de(negocioId, ajuste.getId(), producto.getId(), loteId, sistema,
                    linea.cantidadFisica(), linea.costoUnitario()));
        }
        return comoDto(ajuste);
    }

    /** Criterios 2, 3 y 4: aplica el ajuste, exige motivo y registra al aprobador. */
    @Transactional
    @RequierePermiso("INVENTARIO_AJUSTE_APLICAR")
    public AjusteDelNegocio aplicar(UUID ajusteId) {
        Ajuste ajuste = ajusteDelNegocio(ajusteId);
        ajuste.aplicar(ContextoDeNegocio.actual().usuario());

        for (AjusteLinea linea : lineas.findByAjusteIdOrderById(ajusteId)) {
            BigDecimal diferencia = linea.diferenciaCalculada();
            if (diferencia.signum() == 0) {
                continue;
            }
            TipoMovimiento tipo = diferencia.signum() > 0
                    ? TipoMovimiento.ENTRADA_AJUSTE : TipoMovimiento.SALIDA_AJUSTE;
            libro.registrar(new SolicitudDeMovimiento(linea.getProductoId(), ajuste.getBodegaId(),
                    tipo, diferencia.abs(), OrigenMovimiento.AJUSTE, ajusteId,
                    "Ajuste " + ajuste.getNumero() + " (" + ajuste.getTipo() + "): "
                            + ajuste.getMotivo(),
                    "ajuste:" + ajusteId + ":linea:" + linea.getId(), codigoDeLote(linea), null));
        }
        ajustes.save(ajuste);
        return comoDto(ajuste);
    }

    @Transactional(readOnly = true)
    @RequierePermiso("INVENTARIO_AJUSTE_VER")
    public AjusteDelNegocio ver(UUID ajusteId) {
        return comoDto(ajusteDelNegocio(ajusteId));
    }

    private BigDecimal cantidadDeSistema(UUID productoId, UUID bodegaId, UUID loteId) {
        if (loteId != null) {
            return existenciasLote.findByLoteIdAndBodegaId(loteId, bodegaId)
                    .map(ExistenciaLote::getCantidad).orElse(BigDecimal.ZERO);
        }
        return existencias.findByProductoIdAndBodegaId(productoId, bodegaId)
                .map(Existencia::getCantidad).orElse(BigDecimal.ZERO);
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

    private String codigoDeLote(AjusteLinea linea) {
        if (linea.getLoteId() == null) {
            return null;
        }
        return lotes.findById(linea.getLoteId()).map(Lote::getCodigoLote).orElse(null);
    }

    private Bodega bodegaDelNegocio(UUID bodegaId) {
        Bodega bodega = bodegas.findById(bodegaId)
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

    private Ajuste ajusteDelNegocio(UUID ajusteId) {
        Ajuste ajuste = ajustes.findById(ajusteId)
                .orElseThrow(() -> new NoEncontradoException("Ese ajuste no existe"));
        if (!ajuste.getNegocioId().equals(ContextoDeNegocio.negocioActual())) {
            throw new NoEncontradoException("Ese ajuste no existe");
        }
        return ajuste;
    }

    private AjusteDelNegocio comoDto(Ajuste ajuste) {
        List<LineaDelAjuste> dto = lineas.findByAjusteIdOrderById(ajuste.getId()).stream()
                .map(l -> new LineaDelAjuste(l.getId(), l.getProductoId(), l.getLoteId(),
                        l.getCantidadSistema(), l.getCantidadFisica(), l.diferenciaCalculada()))
                .toList();
        return new AjusteDelNegocio(ajuste.getId(), ajuste.getNumero(), ajuste.getBodegaId(),
                ajuste.getTipo(), ajuste.getEstado(), ajuste.getMotivo(), ajuste.getUsuarioId(),
                ajuste.getAprobadoPor(), ajuste.getFecha(), dto);
    }
}
