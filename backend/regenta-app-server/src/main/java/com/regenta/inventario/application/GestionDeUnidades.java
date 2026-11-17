package com.regenta.inventario.application;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.inventario.domain.UnidadMedida;
import com.regenta.inventario.infrastructure.ProductoRepositorio;
import com.regenta.inventario.infrastructure.UnidadMedidaRepositorio;
import com.regenta.shared.security.RequierePermiso;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.web.NoEncontradoException;
import com.regenta.shared.web.RecursoDuplicadoException;
import com.regenta.shared.web.ReglaDeNegocioException;

/**
 * HU-155 · las unidades de medida del negocio (UND, GLN, KG, MTS): en qué se mide cada producto.
 * Fracciones y detallado (HU-156, HU-161) se apoyan en ellas.
 */
@Service
public class GestionDeUnidades {

    private final UnidadMedidaRepositorio unidades;
    private final ProductoRepositorio productos;

    public GestionDeUnidades(UnidadMedidaRepositorio unidades, ProductoRepositorio productos) {
        this.unidades = unidades;
        this.productos = productos;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("INVENTARIO_PRODUCTO_VER")
    public List<UnidadDelNegocio> listar() {
        return unidades.findByNegocioIdOrderByCodigo(ContextoDeNegocio.negocioActual()).stream()
                .map(UnidadDelNegocio::de).toList();
    }

    @Transactional
    @RequierePermiso("INVENTARIO_PRODUCTO_EDITAR")
    public UnidadDelNegocio crear(SolicitudDeUnidad solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        String codigo = solicitud.codigo().trim().toUpperCase(Locale.ROOT);
        if (unidades.findByNegocioIdAndCodigoIgnoreCase(negocioId, codigo).isPresent()) {
            throw new RecursoDuplicadoException("Ya existe la unidad " + codigo + ".");
        }
        return UnidadDelNegocio.de(unidades.save(UnidadMedida.nueva(negocioId, codigo,
                solicitud.nombre().trim(), solicitud.permiteDecimales())));
    }

    @Transactional
    @RequierePermiso("INVENTARIO_PRODUCTO_EDITAR")
    public UnidadDelNegocio editar(UUID unidadId, SolicitudDeUnidad solicitud) {
        UnidadMedida unidad = delNegocio(unidadId);
        // Un código mal escrito se corrige aquí, sin chocar con el de otra unidad del negocio.
        String codigo = solicitud.codigo().trim().toUpperCase(Locale.ROOT);
        unidades.findByNegocioIdAndCodigoIgnoreCase(unidad.getNegocioId(), codigo)
                .filter(otra -> !otra.getId().equals(unidadId))
                .ifPresent(otra -> {
                    throw new RecursoDuplicadoException("Ya existe la unidad " + codigo + ".");
                });
        unidad.editar(codigo, solicitud.nombre().trim(), solicitud.permiteDecimales());
        return UnidadDelNegocio.de(unidades.save(unidad));
    }

    /** HU-155 C3 · no se desactiva una unidad que todavía usan productos. */
    @Transactional
    @RequierePermiso("INVENTARIO_PRODUCTO_EDITAR")
    public UnidadDelNegocio desactivar(UUID unidadId) {
        UnidadMedida unidad = delNegocio(unidadId);
        long enUso = productos.countByNegocioIdAndUnidadMedidaId(unidad.getNegocioId(), unidadId);
        if (enUso > 0) {
            throw new ReglaDeNegocioException("No se puede desactivar " + unidad.getCodigo()
                    + ": la usan " + enUso + " productos.");
        }
        unidad.desactivar();
        return UnidadDelNegocio.de(unidades.save(unidad));
    }

    private UnidadMedida delNegocio(UUID unidadId) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        return unidades.findById(unidadId).filter(u -> u.getNegocioId().equals(negocioId))
                .orElseThrow(() -> new NoEncontradoException("Esa unidad no existe"));
    }
}
