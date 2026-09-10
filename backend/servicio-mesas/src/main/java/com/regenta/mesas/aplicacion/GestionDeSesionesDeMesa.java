package com.regenta.mesas.aplicacion;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.eventos.RegistroDeEventos;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.mesas.domain.EstadoDeMesa;
import com.regenta.mesas.domain.EstadoDeSesion;
import com.regenta.mesas.domain.Mesa;
import com.regenta.mesas.domain.MesaDeSesion;
import com.regenta.mesas.domain.SesionDeMesa;
import com.regenta.mesas.infra.MesaDeSesionRepositorio;
import com.regenta.mesas.infra.MesaRepositorio;
import com.regenta.mesas.infra.SesionDeMesaRepositorio;

/**
 * Sesiones de mesa (HU-082 y HU-083). Abrir una mesa con el número de comensales
 * la pasa a {@code OCUPADA} y arranca el cronómetro (HU-082 criterio 1); no se
 * abre una segunda encima (criterio 2, con {@code uq_sesion_abierta} de
 * backstop); el cierre de la comanda deja la mesa {@code SUCIA} (criterio 3);
 * marcarla limpia la devuelve a {@code LIBRE} (criterio 4); y la sesión cerrada
 * guarda su duración y sus comensales (criterio 5).
 *
 * <p>Unir mesas (HU-083): dos mesas libres comparten una sola sesión y una sola
 * comanda (criterio 1); una mesa ya ocupada no se une (criterio 2); al cerrar,
 * todas las del grupo pasan a {@code SUCIA} (criterio 3).
 */
@Service
public class GestionDeSesionesDeMesa {

    private static final List<EstadoDeSesion> VIVAS =
            List.of(EstadoDeSesion.ABIERTA, EstadoDeSesion.CUENTA_PEDIDA);

    private final SesionDeMesaRepositorio sesiones;
    private final MesaDeSesionRepositorio grupo;
    private final MesaRepositorio mesas;
    private final RegistroDeEventos eventos;

    public GestionDeSesionesDeMesa(SesionDeMesaRepositorio sesiones, MesaDeSesionRepositorio grupo,
            MesaRepositorio mesas, RegistroDeEventos eventos) {
        this.sesiones = sesiones;
        this.grupo = grupo;
        this.mesas = mesas;
        this.eventos = eventos;
    }

    @Transactional
    @RequierePermiso("MESAS_MESA_EDITAR")
    public SesionDelNegocio abrir(UUID mesaId, SolicitudDeApertura solicitud) {
        Mesa mesa = mesaDelNegocio(mesaId);
        if (mesa.getEstado() != EstadoDeMesa.LIBRE
                || sesiones.existsByMesaPrincipalIdAndEstadoIn(mesaId, VIVAS)
                || grupo.existsByMesaId(mesaId)) {
            throw new ConflictoDeEstadoException("Esa mesa ya tiene una sesión abierta");
        }
        SesionDeMesa sesion = SesionDeMesa.abrir(mesa.getNegocioId(), mesaId,
                solicitud.numComensales(), ContextoDeNegocio.actual().usuario());
        try {
            sesiones.saveAndFlush(sesion);
        } catch (DataIntegrityViolationException choca) {
            throw new ConflictoDeEstadoException("Esa mesa ya tiene una sesión abierta");
        }
        mesa.ocupar();
        mesas.save(mesa);
        publicar("sesion_mesa_abierta", sesion, "mesa_ocupada");
        return dto(sesion);
    }

    /** Une una mesa libre a una sesión existente (HU-083 criterios 1 y 2). */
    @Transactional
    @RequierePermiso("MESAS_MESA_EDITAR")
    public SesionDelNegocio unir(UUID sesionId, UUID mesaId) {
        SesionDeMesa sesion = sesionDelNegocio(sesionId);
        if (!sesion.estaViva()) {
            throw new ConflictoDeEstadoException("Esa sesión ya no está abierta");
        }
        Mesa mesa = mesaDelNegocio(mesaId);
        if (mesaId.equals(sesion.getMesaPrincipalId())
                || grupo.existsBySesionIdAndMesaId(sesionId, mesaId)) {
            return dto(sesion); // ya pertenece al grupo: idempotente
        }
        if (mesa.getEstado() != EstadoDeMesa.LIBRE || grupo.existsByMesaId(mesaId)
                || sesiones.existsByMesaPrincipalIdAndEstadoIn(mesaId, VIVAS)) {
            throw new ConflictoDeEstadoException("Esa mesa ya está ocupada");
        }
        grupo.save(MesaDeSesion.de(sesion.getNegocioId(), sesionId, mesaId));
        mesa.ocupar();
        mesas.save(mesa);
        publicar("mesas_unidas", sesion, "mesa_ocupada");
        return dto(sesion);
    }

    /**
     * Saca una mesa unida del grupo antes de cerrar la cuenta. La mesa queda
     * {@code SUCIA}: alguien estuvo ahí. La principal no se separa.
     */
    @Transactional
    @RequierePermiso("MESAS_MESA_EDITAR")
    public SesionDelNegocio separar(UUID sesionId, UUID mesaId) {
        SesionDeMesa sesion = sesionDelNegocio(sesionId);
        if (mesaId.equals(sesion.getMesaPrincipalId())) {
            throw new ConflictoDeEstadoException(
                    "La mesa principal no se separa; cierra la sesión");
        }
        if (!grupo.existsBySesionIdAndMesaId(sesionId, mesaId)) {
            throw new NoEncontradoException("Esa mesa no está en la sesión");
        }
        grupo.deleteBySesionIdAndMesaId(sesionId, mesaId);
        ensuciarMesa(mesaId);
        publicar("mesa_separada", sesion, "mesa_por_limpiar");
        return dto(sesion);
    }

    @Transactional(readOnly = true)
    @RequierePermiso("MESAS_MESA_VER")
    public SesionDelNegocio ver(UUID sesionId) {
        return dto(sesionDelNegocio(sesionId));
    }

    /** La sesión viva de una mesa (HU-082 criterio 1), sea principal o unida. */
    @Transactional(readOnly = true)
    @RequierePermiso("MESAS_MESA_VER")
    public SesionDelNegocio sesionActual(UUID mesaId) {
        mesaDelNegocio(mesaId);
        SesionDeMesa sesion = sesionVivaDeMesa(mesaId)
                .orElseThrow(() -> new NoEncontradoException(
                        "Esa mesa no tiene una sesión abierta"));
        return dto(sesion);
    }

    private java.util.Optional<SesionDeMesa> sesionVivaDeMesa(UUID mesaId) {
        var comoPrincipal = sesiones
                .findFirstByMesaPrincipalIdAndEstadoInOrderByAbiertaEnDesc(mesaId, VIVAS);
        if (comoPrincipal.isPresent()) {
            return comoPrincipal;
        }
        return grupo.findByMesaId(mesaId).stream().findFirst()
                .flatMap(m -> sesiones.findById(m.getSesionId()))
                .filter(SesionDeMesa::estaViva);
    }

    @Transactional(readOnly = true)
    @RequierePermiso("MESAS_MESA_VER")
    public List<SesionDelNegocio> historial(UUID mesaId) {
        mesaDelNegocio(mesaId);
        return sesiones
                .findByMesaPrincipalIdAndNegocioIdOrderByAbiertaEnDesc(
                        mesaId, ContextoDeNegocio.negocioActual())
                .stream().map(this::dto).toList();
    }

    @Transactional
    @RequierePermiso("MESAS_MESA_EDITAR")
    public SesionDelNegocio pedirCuenta(UUID sesionId) {
        SesionDeMesa sesion = sesionDelNegocio(sesionId);
        sesion.pedirCuenta();
        sesiones.save(sesion);
        Mesa mesa = mesaDelNegocio(sesion.getMesaPrincipalId());
        mesa.pedirCuenta();
        mesas.save(mesa);
        publicar("sesion_mesa_cuenta_pedida", sesion, "mesa_cuenta_pedida");
        return dto(sesion);
    }

    /** Cierra la sesión a mano y deja las mesas por limpiar (HU-082 c3, HU-083 c3). */
    @Transactional
    @RequierePermiso("MESAS_MESA_EDITAR")
    public SesionDelNegocio cerrar(UUID sesionId) {
        SesionDeMesa sesion = sesionDelNegocio(sesionId);
        cerrarYEnsuciar(sesion);
        return dto(sesion);
    }

    /** Marca la mesa limpia: {@code SUCIA -> LIBRE} (HU-082 criterio 4). */
    @Transactional
    @RequierePermiso("MESAS_MESA_EDITAR")
    public void marcarLimpia(UUID mesaId) {
        Mesa mesa = mesaDelNegocio(mesaId);
        mesa.limpiar();
        mesas.save(mesa);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("negocio_id", mesa.getNegocioId().toString());
        payload.put("mesa_id", mesaId.toString());
        eventos.registrar(mesa.getNegocioId(), "Mesa", mesaId, "mesa_liberada", payload);
    }

    /**
     * Cierra la sesión de una comanda que terminó (HU-082 criterio 3). Interno:
     * lo llama {@code ConsumidorDeComandasCerradas} dentro de la transacción del
     * Inbox, sin permiso de usuario.
     */
    @Transactional
    public void cerrarPorComanda(UUID negocioId, UUID sesionId, UUID mesaId, UUID comandaId) {
        SesionDeMesa sesion = resolverSesion(negocioId, sesionId, mesaId, comandaId);
        if (sesion == null || !sesion.estaViva()) {
            return;
        }
        if (comandaId != null && sesion.getComandaId() == null) {
            sesion.asignarComanda(comandaId);
        }
        cerrarYEnsuciar(sesion);
    }

    private void cerrarYEnsuciar(SesionDeMesa sesion) {
        sesion.cerrar(OffsetDateTime.now());
        sesiones.save(sesion);
        for (UUID mesaId : mesasDelGrupo(sesion)) {
            ensuciarMesa(mesaId);
        }
        publicar("sesion_mesa_cerrada", sesion, "mesa_por_limpiar");
    }

    private void ensuciarMesa(UUID mesaId) {
        mesas.findByIdAndNegocioId(mesaId, ContextoDeNegocio.negocioActual())
                .ifPresent(m -> {
                    m.ensuciar();
                    mesas.save(m);
                });
    }

    private List<UUID> mesasDelGrupo(SesionDeMesa sesion) {
        List<UUID> ids = new ArrayList<>();
        ids.add(sesion.getMesaPrincipalId());
        grupo.findBySesionId(sesion.getId()).stream().map(MesaDeSesion::getMesaId).forEach(ids::add);
        return ids;
    }

    private SesionDelNegocio dto(SesionDeMesa sesion) {
        return SesionDelNegocio.de(sesion, mesasDelGrupo(sesion));
    }

    private SesionDeMesa resolverSesion(UUID negocioId, UUID sesionId, UUID mesaId,
            UUID comandaId) {
        if (sesionId != null) {
            return sesiones.findByIdAndNegocioId(sesionId, negocioId).orElse(null);
        }
        if (comandaId != null) {
            var porComanda = sesiones
                    .findFirstByComandaIdAndEstadoInOrderByAbiertaEnDesc(comandaId, VIVAS);
            if (porComanda.isPresent()) {
                return porComanda.get();
            }
        }
        if (mesaId != null) {
            return sesionVivaDeMesa(mesaId).orElse(null);
        }
        return null;
    }

    private void publicar(String eventoSesion, SesionDeMesa sesion, String eventoMesa) {
        List<UUID> mesaIds = mesasDelGrupo(sesion);
        Map<String, Object> sesionPayload = new LinkedHashMap<>();
        sesionPayload.put("negocio_id", sesion.getNegocioId().toString());
        sesionPayload.put("sesion_id", sesion.getId().toString());
        sesionPayload.put("mesa_id", sesion.getMesaPrincipalId().toString());
        sesionPayload.put("mesa_ids", mesaIds.stream().map(UUID::toString).toList());
        sesionPayload.put("estado", sesion.getEstado().name());
        sesionPayload.put("num_comensales", (int) sesion.getNumComensales());
        eventos.registrar(sesion.getNegocioId(), "SesionDeMesa", sesion.getId(), eventoSesion,
                sesionPayload);

        Map<String, Object> mesaPayload = new LinkedHashMap<>();
        mesaPayload.put("negocio_id", sesion.getNegocioId().toString());
        mesaPayload.put("mesa_id", sesion.getMesaPrincipalId().toString());
        mesaPayload.put("mesa_ids", mesaIds.stream().map(UUID::toString).toList());
        mesaPayload.put("sesion_id", sesion.getId().toString());
        eventos.registrar(sesion.getNegocioId(), "Mesa", sesion.getMesaPrincipalId(), eventoMesa,
                mesaPayload);
    }

    private Mesa mesaDelNegocio(UUID mesaId) {
        return mesas.findByIdAndNegocioId(mesaId, ContextoDeNegocio.negocioActual())
                .filter(Mesa::isActiva)
                .orElseThrow(() -> new NoEncontradoException("Esa mesa no existe"));
    }

    private SesionDeMesa sesionDelNegocio(UUID sesionId) {
        return sesiones.findByIdAndNegocioId(sesionId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Esa sesión no existe"));
    }
}
