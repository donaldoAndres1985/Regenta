package com.regenta.mesas.aplicacion;

import java.time.OffsetDateTime;
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
import com.regenta.mesas.domain.EstadoDeSesion;
import com.regenta.mesas.domain.Mesa;
import com.regenta.mesas.domain.SesionDeMesa;
import com.regenta.mesas.infra.MesaRepositorio;
import com.regenta.mesas.infra.SesionDeMesaRepositorio;

/**
 * Sesiones de mesa (HU-082). Abrir una mesa con el número de comensales la pasa
 * a {@code OCUPADA} y arranca el cronómetro (criterio 1); no se puede abrir una
 * segunda encima (criterio 2, además con {@code uq_sesion_abierta} de backstop);
 * el cierre de la comanda deja la mesa {@code SUCIA} (criterio 3); marcarla
 * limpia la devuelve a {@code LIBRE} (criterio 4); y la sesión cerrada guarda su
 * duración y sus comensales (criterio 5).
 */
@Service
public class GestionDeSesionesDeMesa {

    private static final List<EstadoDeSesion> VIVAS =
            List.of(EstadoDeSesion.ABIERTA, EstadoDeSesion.CUENTA_PEDIDA);

    private final SesionDeMesaRepositorio sesiones;
    private final MesaRepositorio mesas;
    private final RegistroDeEventos eventos;

    public GestionDeSesionesDeMesa(SesionDeMesaRepositorio sesiones, MesaRepositorio mesas,
            RegistroDeEventos eventos) {
        this.sesiones = sesiones;
        this.mesas = mesas;
        this.eventos = eventos;
    }

    @Transactional
    @RequierePermiso("MESAS_MESA_EDITAR")
    public SesionDelNegocio abrir(UUID mesaId, SolicitudDeApertura solicitud) {
        Mesa mesa = mesaDelNegocio(mesaId);
        if (sesiones.existsByMesaPrincipalIdAndEstadoIn(mesaId, VIVAS)) {
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
        return SesionDelNegocio.de(sesion);
    }

    @Transactional(readOnly = true)
    @RequierePermiso("MESAS_MESA_VER")
    public SesionDelNegocio ver(UUID sesionId) {
        return SesionDelNegocio.de(sesionDelNegocio(sesionId));
    }

    /** La sesión viva de una mesa (HU-082 criterio 1). 404 si la mesa está libre. */
    @Transactional(readOnly = true)
    @RequierePermiso("MESAS_MESA_VER")
    public SesionDelNegocio sesionActual(UUID mesaId) {
        mesaDelNegocio(mesaId);
        return sesiones
                .findFirstByMesaPrincipalIdAndEstadoInOrderByAbiertaEnDesc(mesaId, VIVAS)
                .map(SesionDelNegocio::de)
                .orElseThrow(() -> new NoEncontradoException("Esa mesa no tiene una sesión abierta"));
    }

    @Transactional(readOnly = true)
    @RequierePermiso("MESAS_MESA_VER")
    public List<SesionDelNegocio> historial(UUID mesaId) {
        mesaDelNegocio(mesaId);
        return sesiones
                .findByMesaPrincipalIdAndNegocioIdOrderByAbiertaEnDesc(
                        mesaId, ContextoDeNegocio.negocioActual())
                .stream().map(SesionDelNegocio::de).toList();
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
        return SesionDelNegocio.de(sesion);
    }

    /** Cierra la sesión a mano y deja la mesa por limpiar (HU-082 criterio 3). */
    @Transactional
    @RequierePermiso("MESAS_MESA_EDITAR")
    public SesionDelNegocio cerrar(UUID sesionId) {
        SesionDeMesa sesion = sesionDelNegocio(sesionId);
        cerrarYEnsuciar(sesion);
        return SesionDelNegocio.de(sesion);
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
        Mesa mesa = mesaDelNegocio(sesion.getMesaPrincipalId());
        mesa.ensuciar();
        mesas.save(mesa);
        publicar("sesion_mesa_cerrada", sesion, "mesa_por_limpiar");
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
            return sesiones
                    .findFirstByMesaPrincipalIdAndEstadoInOrderByAbiertaEnDesc(mesaId, VIVAS)
                    .orElse(null);
        }
        return null;
    }

    private void publicar(String eventoSesion, SesionDeMesa sesion, String eventoMesa) {
        Map<String, Object> sesionPayload = new LinkedHashMap<>();
        sesionPayload.put("negocio_id", sesion.getNegocioId().toString());
        sesionPayload.put("sesion_id", sesion.getId().toString());
        sesionPayload.put("mesa_id", sesion.getMesaPrincipalId().toString());
        sesionPayload.put("estado", sesion.getEstado().name());
        sesionPayload.put("num_comensales", (int) sesion.getNumComensales());
        eventos.registrar(sesion.getNegocioId(), "SesionDeMesa", sesion.getId(), eventoSesion,
                sesionPayload);

        Map<String, Object> mesaPayload = new LinkedHashMap<>();
        mesaPayload.put("negocio_id", sesion.getNegocioId().toString());
        mesaPayload.put("mesa_id", sesion.getMesaPrincipalId().toString());
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
