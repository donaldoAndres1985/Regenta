package com.regenta.mesas.aplicacion;

import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.mesas.domain.Zona;
import com.regenta.mesas.infra.MesaRepositorio;
import com.regenta.mesas.infra.ZonaRepositorio;

/**
 * Zonas del salón (HU-081): el administrador crea "Salón", "Terraza", "Barra" y
 * las ordena para dibujar el plano. No hay permiso propio de zona en el
 * catálogo: se reusan los de mesa.
 */
@Service
public class GestionDeZonas {

    private final ZonaRepositorio zonas;
    private final MesaRepositorio mesas;

    public GestionDeZonas(ZonaRepositorio zonas, MesaRepositorio mesas) {
        this.zonas = zonas;
        this.mesas = mesas;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("MESAS_MESA_VER")
    public List<ZonaDelNegocio> listar() {
        return zonas.findByNegocioIdOrderByOrdenAscNombreAsc(ContextoDeNegocio.negocioActual())
                .stream().map(ZonaDelNegocio::de).toList();
    }

    @Transactional(readOnly = true)
    @RequierePermiso("MESAS_MESA_VER")
    public ZonaDelNegocio ver(UUID zonaId) {
        return ZonaDelNegocio.de(delNegocio(zonaId));
    }

    @Transactional
    @RequierePermiso("MESAS_MESA_CREAR")
    public ZonaDelNegocio crear(SolicitudDeZona solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        String nombre = solicitud.nombre().trim();
        if (zonas.existsByNegocioIdAndNombreIgnoreCase(negocioId, nombre)) {
            throw new RecursoDuplicadoException("Ya hay una zona llamada " + nombre);
        }
        Zona zona = Zona.crear(negocioId, null, nombre,
                solicitud.orden() == null ? proximoOrden(negocioId) : solicitud.orden(),
                solicitud.color());
        try {
            zonas.save(zona);
        } catch (DataIntegrityViolationException choca) {
            throw new RecursoDuplicadoException("Ya hay una zona llamada " + nombre);
        }
        return ZonaDelNegocio.de(zona);
    }

    @Transactional
    @RequierePermiso("MESAS_MESA_EDITAR")
    public ZonaDelNegocio actualizar(UUID zonaId, SolicitudDeZona solicitud) {
        Zona zona = delNegocio(zonaId);
        zona.editar(solicitud.nombre().trim(), solicitud.orden(), solicitud.color());
        zonas.save(zona);
        return ZonaDelNegocio.de(zona);
    }

    @Transactional
    @RequierePermiso("MESAS_MESA_EDITAR")
    public ZonaDelNegocio cambiarActivacion(UUID zonaId, boolean activa) {
        Zona zona = delNegocio(zonaId);
        if (activa) {
            zona.activar();
        } else {
            zona.desactivar();
        }
        zonas.save(zona);
        return ZonaDelNegocio.de(zona);
    }

    /** Fija el orden de las zonas según la lista recibida. */
    @Transactional
    @RequierePermiso("MESAS_MESA_EDITAR")
    public List<ZonaDelNegocio> reordenar(List<UUID> ordenIds) {
        List<Zona> todas = zonas.findByNegocioIdOrderByOrdenAscNombreAsc(
                ContextoDeNegocio.negocioActual());
        for (Zona z : todas) {
            int pos = ordenIds == null ? -1 : ordenIds.indexOf(z.getId());
            if (pos >= 0) {
                z.reordenar(pos);
            }
        }
        zonas.saveAll(todas);
        return zonas.findByNegocioIdOrderByOrdenAscNombreAsc(ContextoDeNegocio.negocioActual())
                .stream().map(ZonaDelNegocio::de).toList();
    }

    @Transactional
    @RequierePermiso("MESAS_MESA_EDITAR")
    public void eliminar(UUID zonaId) {
        Zona zona = delNegocio(zonaId);
        if (mesas.countByNegocioIdAndZonaIdAndActivaTrue(zona.getNegocioId(), zonaId) > 0) {
            throw new ConflictoDeEstadoException(
                    "La zona tiene mesas; muévelas a otra zona antes de borrarla");
        }
        zonas.delete(zona);
    }

    private int proximoOrden(UUID negocioId) {
        return zonas.findByNegocioIdOrderByOrdenAscNombreAsc(negocioId).stream()
                .mapToInt(Zona::getOrden).max().orElse(-1) + 1;
    }

    private Zona delNegocio(UUID zonaId) {
        return zonas.findByIdAndNegocioId(zonaId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Esa zona no existe"));
    }
}
