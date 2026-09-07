package com.regenta.inventario.aplicacion;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.inventario.domain.Bodega;
import com.regenta.inventario.infra.BodegaRepositorio;
import com.regenta.inventario.infra.ExistenciaRepositorio;

/**
 * Las bodegas del negocio. HU-029.
 *
 * <p>Un negocio siempre tiene al menos una: la principal por defecto se asegura
 * al primer contacto con el inventario (criterio 3). Una bodega con existencias
 * no se elimina (criterio 4).
 */
@Service
public class GestionDeBodegas {

    private final BodegaRepositorio bodegas;
    private final ExistenciaRepositorio existencias;

    public GestionDeBodegas(BodegaRepositorio bodegas, ExistenciaRepositorio existencias) {
        this.bodegas = bodegas;
        this.existencias = existencias;
    }

    /**
     * Criterio 3: garantiza que el negocio tenga su bodega principal. Idempotente
     * -- llamarla dos veces no crea dos.
     */
    @Transactional
    public Bodega asegurarBodegaPorDefecto() {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        return bodegas.findFirstByNegocioIdAndEsDefaultTrue(negocioId)
                .orElseGet(() -> bodegas.existsByNegocioId(negocioId)
                        ? bodegas.findByNegocioIdOrderByCodigo(negocioId).get(0)
                        : bodegas.save(Bodega.principalPorDefecto(negocioId)));
    }

    @Transactional(readOnly = true)
    @RequierePermiso("INVENTARIO_BODEGA_VER")
    public List<BodegaDelNegocio> listar() {
        return bodegas.findByNegocioIdOrderByCodigo(ContextoDeNegocio.negocioActual()).stream()
                .map(GestionDeBodegas::comoDto)
                .toList();
    }

    @Transactional
    @RequierePermiso("INVENTARIO_BODEGA_CREAR")
    public BodegaDelNegocio crear(SolicitudDeBodega solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        bodegas.findByNegocioIdAndCodigo(negocioId, solicitud.codigo()).ifPresent(existente -> {
            throw new RecursoDuplicadoException(
                    "Ya hay una bodega con el codigo " + solicitud.codigo());
        });
        Bodega bodega = Bodega.nueva(negocioId, solicitud.codigo(), solicitud.nombre(),
                solicitud.tipo(), false);
        bodega.renombrar(solicitud.nombre(), solicitud.sucursalId());
        bodegas.save(bodega);
        return comoDto(bodega);
    }

    /** Criterio 4: con existencias dentro, no se elimina. */
    @Transactional
    @RequierePermiso("INVENTARIO_BODEGA_ELIMINAR")
    public void eliminar(UUID bodegaId) {
        Bodega bodega = buscar(bodegaId);
        if (existencias.existsByBodegaId(bodegaId)) {
            throw new ConflictoDeEstadoException("La bodega \"" + bodega.getNombre()
                    + "\" tiene existencias. Trasladalas o ajustalas a cero antes de eliminarla");
        }
        bodegas.delete(bodega);
    }

    private Bodega buscar(UUID bodegaId) {
        Bodega bodega = bodegas.findById(bodegaId)
                .orElseThrow(() -> new NoEncontradoException("Esa bodega no existe"));
        if (!bodega.getNegocioId().equals(ContextoDeNegocio.negocioActual())) {
            throw new NoEncontradoException("Esa bodega no existe");
        }
        return bodega;
    }

    private static BodegaDelNegocio comoDto(Bodega b) {
        return new BodegaDelNegocio(b.getId(), b.getCodigo(), b.getNombre(), b.getTipo(),
                b.getSucursalId(), b.isEsDefault(), b.isActiva());
    }
}
