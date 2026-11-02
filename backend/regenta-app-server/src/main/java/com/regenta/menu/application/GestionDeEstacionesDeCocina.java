package com.regenta.menu.application;

import com.regenta.menu.api.*;

import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.shared.web.NoEncontradoException;
import com.regenta.shared.web.RecursoDuplicadoException;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.security.RequierePermiso;
import com.regenta.menu.domain.EstacionDeCocina;
import com.regenta.menu.infrastructure.EstacionDeCocinaRepositorio;

/** Estaciones de cocina (HU-077): parrilla, frÃ­a, bar. El cÃ³digo es Ãºnico por negocio. */
@Service
public class GestionDeEstacionesDeCocina {

    private final EstacionDeCocinaRepositorio estaciones;

    public GestionDeEstacionesDeCocina(EstacionDeCocinaRepositorio estaciones) {
        this.estaciones = estaciones;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("MENU_PLATO_VER")
    public List<EstacionDelNegocio> listar() {
        return estaciones.findByNegocioIdOrderByOrdenAscNombreAsc(ContextoDeNegocio.negocioActual())
                .stream().map(EstacionDelNegocio::de).toList();
    }

    @Transactional(readOnly = true)
    @RequierePermiso("MENU_PLATO_VER")
    public EstacionDelNegocio ver(UUID estacionId) {
        return EstacionDelNegocio.de(delNegocio(estacionId));
    }

    @Transactional
    @RequierePermiso("MENU_PLATO_CREAR")
    public EstacionDelNegocio crear(SolicitudDeEstacion solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        String codigo = solicitud.codigo().trim().toUpperCase(java.util.Locale.ROOT);
        if (estaciones.existsByNegocioIdAndCodigo(negocioId, codigo)) {
            throw new RecursoDuplicadoException("Ya hay una estaciÃ³n con el cÃ³digo " + codigo);
        }
        EstacionDeCocina estacion = EstacionDeCocina.crear(negocioId, solicitud.sucursalId(),
                codigo, solicitud.nombre().trim(), solicitud.impresora(),
                solicitud.orden() == null ? 0 : solicitud.orden());
        try {
            estaciones.save(estacion);
        } catch (DataIntegrityViolationException choca) {
            throw new RecursoDuplicadoException("Ya hay una estaciÃ³n con el cÃ³digo " + codigo);
        }
        return EstacionDelNegocio.de(estacion);
    }

    @Transactional
    @RequierePermiso("MENU_PLATO_EDITAR")
    public EstacionDelNegocio actualizar(UUID estacionId, SolicitudDeEstacion solicitud) {
        EstacionDeCocina estacion = delNegocio(estacionId);
        estacion.editar(solicitud.nombre().trim(), solicitud.impresora(),
                solicitud.orden() == null ? estacion.getOrden() : solicitud.orden());
        estaciones.save(estacion);
        return EstacionDelNegocio.de(estacion);
    }

    @Transactional
    @RequierePermiso("MENU_PLATO_EDITAR")
    public EstacionDelNegocio cambiarActivacion(UUID estacionId, boolean activa) {
        EstacionDeCocina estacion = delNegocio(estacionId);
        if (activa) {
            estacion.activar();
        } else {
            estacion.desactivar();
        }
        estaciones.save(estacion);
        return EstacionDelNegocio.de(estacion);
    }

    private EstacionDeCocina delNegocio(UUID estacionId) {
        return estaciones.findByIdAndNegocioId(estacionId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Esa estaciÃ³n no existe"));
    }
}
