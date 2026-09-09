package com.regenta.reservas.aplicacion;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
import com.regenta.reservas.domain.EstadoReserva;
import com.regenta.reservas.domain.Estancia;
import com.regenta.reservas.domain.Ocupante;
import com.regenta.reservas.domain.Reserva;
import com.regenta.reservas.infra.RepositorioDeEstancias;
import com.regenta.reservas.infra.RepositorioDeOcupantes;
import com.regenta.reservas.infra.RepositorioDeReservas;

/**
 * Check-in y estancia (HU-072). Al llegar el huésped se asigna la habitación
 * concreta —la que pida la recepción o una libre de ese tipo—, la reserva pasa a
 * {@code CHECK_IN}, se abre la estancia y se registran los ocupantes. Que el
 * recurso no esté ya ocupado lo comprueba el {@code EXCLUDE} de la tabla, no un
 * chequeo en Java (criterio 2).
 */
@Service
public class GestionDeEstancias {

    private final RepositorioDeReservas reservas;
    private final RepositorioDeEstancias estancias;
    private final RepositorioDeOcupantes ocupantes;
    private final ConsultaDeDisponibilidad disponibilidad;
    private final RegistroDeEventos eventos;

    public GestionDeEstancias(RepositorioDeReservas reservas, RepositorioDeEstancias estancias,
            RepositorioDeOcupantes ocupantes, ConsultaDeDisponibilidad disponibilidad,
            RegistroDeEventos eventos) {
        this.reservas = reservas;
        this.estancias = estancias;
        this.ocupantes = ocupantes;
        this.disponibilidad = disponibilidad;
        this.eventos = eventos;
    }

    @Transactional
    @RequierePermiso("RESERVAS_RESERVA_EDITAR")
    public EstanciaDelNegocio checkIn(UUID reservaId, SolicitudDeCheckIn solicitud) {
        Reserva reserva = delNegocio(reservaId);
        if (reserva.getEstado() != EstadoReserva.CONFIRMADA) {
            throw new ConflictoDeEstadoException(
                    "Solo se hace check-in de una reserva confirmada (está " + reserva.getEstado()
                            + ")");
        }
        if (estancias.porReserva(reservaId).isPresent()) {
            throw new ConflictoDeEstadoException("La reserva ya tiene el check-in hecho");
        }

        UUID recurso = resolverRecurso(reserva, solicitud.recursoId());
        Reserva conCheckIn = reserva.checkIn(recurso);
        try {
            if (!reservas.actualizarCheckIn(conCheckIn, reserva.getVersion())) {
                throw new ConflictoDeEstadoException(
                        "La reserva cambió mientras se procesaba; volvé a intentarlo");
            }
        } catch (DataIntegrityViolationException choca) {
            if (esSolapeDeRecurso(choca)) {
                throw new ConflictoDeEstadoException(
                        "Ese recurso ya está ocupado en el periodo de la reserva");
            }
            throw choca;
        }

        UUID usuarioId = ContextoDeNegocio.usuarioActual();
        Estancia estancia = Estancia.abrir(reserva.getNegocioId(), reservaId, recurso,
                OffsetDateTime.now(ZoneOffset.UTC), usuarioId, reserva.getHasta(),
                solicitud.deposito(), solicitud.observacionesEntrada());
        estancias.crear(estancia);

        registrarOcupantes(reserva.getNegocioId(), reservaId, solicitud.ocupantes());

        reservas.registrarEvento(reserva.getNegocioId(), reservaId, EstadoReserva.CONFIRMADA,
                EstadoReserva.CHECK_IN, usuarioId, "Check-in en el recurso " + recurso);
        publicarCheckIn(reserva, estancia, recurso);

        return EstanciaDelNegocio.de(estancia, ocupantes.porReserva(reservaId));
    }

    @Transactional
    @RequierePermiso("RESERVAS_RESERVA_EDITAR")
    public OcupanteDelNegocio agregarOcupante(UUID reservaId, SolicitudDeOcupante solicitud) {
        Reserva reserva = delNegocio(reservaId);
        Ocupante ocupante = aOcupante(reserva.getNegocioId(), reservaId, solicitud);
        ocupantes.agregar(ocupante);
        return OcupanteDelNegocio.de(ocupante);
    }

    @Transactional(readOnly = true)
    @RequierePermiso("RESERVAS_RESERVA_VER")
    public EstanciaDelNegocio verEstancia(UUID reservaId) {
        delNegocio(reservaId);
        Estancia estancia = estancias.porReserva(reservaId)
                .orElseThrow(() -> new NoEncontradoException("Esa reserva no tiene estancia"));
        return EstanciaDelNegocio.de(estancia, ocupantes.porReserva(reservaId));
    }

    private UUID resolverRecurso(Reserva reserva, UUID pedido) {
        if (pedido != null) {
            return pedido;
        }
        if (reserva.getRecursoId() != null) {
            return reserva.getRecursoId();
        }
        List<DisponibilidadEnPeriodo.RecursoLibre> libres = disponibilidad.consultar(
                new SolicitudDeDisponibilidad(reserva.getDesde(), reserva.getHasta(),
                        reserva.getTipoRecursoId(), reserva.numPersonas())).libres();
        if (libres.isEmpty()) {
            throw new ConflictoDeEstadoException(
                    "No hay recursos libres de ese tipo para el periodo de la reserva");
        }
        return libres.get(0).recursoId();
    }

    private void registrarOcupantes(UUID negocioId, UUID reservaId,
            List<SolicitudDeOcupante> solicitados) {
        if (solicitados == null) {
            return;
        }
        for (SolicitudDeOcupante s : solicitados) {
            ocupantes.agregar(aOcupante(negocioId, reservaId, s));
        }
    }

    private static Ocupante aOcupante(UUID negocioId, UUID reservaId, SolicitudDeOcupante s) {
        return Ocupante.nuevo(negocioId, reservaId, s.esTitular(), s.nombres(), s.apellidos(),
                s.tipoDocumento(), s.numeroDocumento(), s.nacionalidad(), s.fechaNacimiento(),
                s.telefono(), s.email());
    }

    private Reserva delNegocio(UUID reservaId) {
        return reservas.buscar(reservaId)
                .filter(r -> r.getNegocioId().equals(ContextoDeNegocio.negocioActual()))
                .orElseThrow(() -> new NoEncontradoException("Esa reserva no existe"));
    }

    private static boolean esSolapeDeRecurso(DataIntegrityViolationException e) {
        String mensaje = e.getMostSpecificCause().getMessage();
        return mensaje != null && mensaje.contains(RepositorioDeReservas.CONSTRAINT_SOLAPE);
    }

    private void publicarCheckIn(Reserva reserva, Estancia estancia, UUID recurso) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("negocio_id", reserva.getNegocioId().toString());
        payload.put("reserva_id", reserva.getId().toString());
        payload.put("estancia_id", estancia.getId().toString());
        payload.put("recurso_id", recurso.toString());
        payload.put("tipo_recurso_id", reserva.getTipoRecursoId().toString());
        payload.put("check_in_en", estancia.getCheckInEn().toString());
        payload.put("check_out_previsto", estancia.getCheckOutPrevisto().toString());
        eventos.registrar(reserva.getNegocioId(), "Reserva", reserva.getId(),
                "check_in_registrado", payload);
    }
}
