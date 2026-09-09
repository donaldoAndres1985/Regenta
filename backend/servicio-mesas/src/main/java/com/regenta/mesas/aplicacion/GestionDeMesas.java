package com.regenta.mesas.aplicacion;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.mesas.aplicacion.PlanoDelSalon.ZonaConMesas;
import com.regenta.mesas.domain.FormaDeMesa;
import com.regenta.mesas.domain.Mesa;
import com.regenta.mesas.domain.Zona;
import com.regenta.mesas.infra.ConteoDeSesionesAbiertas;
import com.regenta.mesas.infra.MesaRepositorio;
import com.regenta.mesas.infra.ZonaRepositorio;

/**
 * Mesas del salón (HU-081). Cada una con su código, capacidad, zona y posición
 * en el plano (criterio 1); el código es único por negocio (criterio 2); mover
 * una mesa guarda su posición y se ve igual la próxima vez (criterio 3); una
 * mesa con una sesión abierta no se puede eliminar (criterio 4).
 */
@Service
public class GestionDeMesas {

    private final MesaRepositorio mesas;
    private final ZonaRepositorio zonas;
    private final ConteoDeSesionesAbiertas sesiones;

    public GestionDeMesas(MesaRepositorio mesas, ZonaRepositorio zonas,
            ConteoDeSesionesAbiertas sesiones) {
        this.mesas = mesas;
        this.zonas = zonas;
        this.sesiones = sesiones;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("MESAS_MESA_VER")
    public List<MesaDelNegocio> listar(UUID zonaId) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        List<Mesa> lista = zonaId == null
                ? mesas.findByNegocioIdAndActivaTrueOrderByCodigoAsc(negocioId)
                : mesas.findByNegocioIdAndZonaIdAndActivaTrueOrderByCodigoAsc(negocioId, zonaId);
        return lista.stream().map(MesaDelNegocio::de).toList();
    }

    @Transactional(readOnly = true)
    @RequierePermiso("MESAS_MESA_VER")
    public MesaDelNegocio ver(UUID mesaId) {
        return MesaDelNegocio.de(delNegocio(mesaId));
    }

    /** El plano completo: zonas en orden, cada una con sus mesas (HU-081). */
    @Transactional(readOnly = true)
    @RequierePermiso("MESAS_MESA_VER")
    public PlanoDelSalon plano() {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        List<Zona> zs = zonas.findByNegocioIdOrderByOrdenAscNombreAsc(negocioId);
        Map<UUID, List<MesaDelNegocio>> porZona = mesas
                .findByNegocioIdAndActivaTrueOrderByCodigoAsc(negocioId).stream()
                .map(MesaDelNegocio::de)
                .collect(Collectors.groupingBy(
                        m -> m.zonaId() == null ? SIN_ZONA : m.zonaId()));
        List<ZonaConMesas> bloques = zs.stream()
                .map(z -> new ZonaConMesas(ZonaDelNegocio.de(z),
                        porZona.getOrDefault(z.getId(), List.of())))
                .toList();
        return new PlanoDelSalon(bloques, porZona.getOrDefault(SIN_ZONA, List.of()));
    }

    @Transactional
    @RequierePermiso("MESAS_MESA_CREAR")
    public MesaDelNegocio crear(SolicitudDeMesa solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        exigirZona(solicitud.zonaId(), negocioId);
        String codigo = solicitud.codigo().trim().toUpperCase();
        if (mesas.existsByNegocioIdAndCodigo(negocioId, codigo)) {
            throw new RecursoDuplicadoException("Ya hay una mesa con el código " + codigo);
        }
        Mesa mesa = Mesa.crear(negocioId, null, solicitud.zonaId(), codigo, solicitud.nombre(),
                solicitud.capacidad(), FormaDeMesa.desde(solicitud.forma()), solicitud.posX(),
                solicitud.posY(), solicitud.ancho(), solicitud.alto());
        try {
            mesas.save(mesa);
        } catch (DataIntegrityViolationException choca) {
            throw new RecursoDuplicadoException("Ya hay una mesa con el código " + codigo);
        }
        return MesaDelNegocio.de(mesa);
    }

    @Transactional
    @RequierePermiso("MESAS_MESA_EDITAR")
    public MesaDelNegocio actualizar(UUID mesaId, SolicitudDeMesa solicitud) {
        Mesa mesa = delNegocio(mesaId);
        exigirZona(solicitud.zonaId(), mesa.getNegocioId());
        mesa.editar(solicitud.zonaId(), solicitud.nombre(), solicitud.capacidad(),
                FormaDeMesa.desde(solicitud.forma()));
        mesas.save(mesa);
        return MesaDelNegocio.de(mesa);
    }

    /** Guarda la nueva posición de la mesa en el plano (HU-081 criterio 3). */
    @Transactional
    @RequierePermiso("MESAS_MESA_EDITAR")
    public MesaDelNegocio mover(UUID mesaId, SolicitudDePosicion solicitud) {
        Mesa mesa = delNegocio(mesaId);
        mesa.mover(solicitud.posX(), solicitud.posY(), solicitud.ancho(), solicitud.alto());
        mesas.save(mesa);
        return MesaDelNegocio.de(mesa);
    }

    @Transactional
    @RequierePermiso("MESAS_MESA_EDITAR")
    public void eliminar(UUID mesaId) {
        Mesa mesa = delNegocio(mesaId);
        if (sesiones.laMesaTieneSesionViva(mesaId)) {
            throw new ConflictoDeEstadoException(
                    "La mesa tiene una sesión abierta; ciérrala antes de borrarla");
        }
        mesa.desactivar();
        mesas.save(mesa);
    }

    private void exigirZona(UUID zonaId, UUID negocioId) {
        if (zonaId != null && zonas.findByIdAndNegocioId(zonaId, negocioId).isEmpty()) {
            throw new NoEncontradoException("Esa zona no existe");
        }
    }

    private Mesa delNegocio(UUID mesaId) {
        return mesas.findByIdAndNegocioId(mesaId, ContextoDeNegocio.negocioActual())
                .filter(Mesa::isActiva)
                .orElseThrow(() -> new NoEncontradoException("Esa mesa no existe"));
    }

    private static final UUID SIN_ZONA = new UUID(0L, 0L);
}
