package com.regenta.reservas.aplicacion;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.eventos.RegistroDeEventos;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.reservas.domain.CanalReserva;
import com.regenta.reservas.domain.Reserva;
import com.regenta.reservas.infra.AsignadorDeConsecutivos;
import com.regenta.reservas.infra.RepositorioDeReservas;

/**
 * Crear reservas sin overbooking (HU-070). El anti-overbooking NO se comprueba
 * en Java —un "¿está libre?" seguido de un INSERT tiene ventana de carrera—: lo
 * garantiza el {@code EXCLUDE USING gist} sobre {@code periodo} de la tabla, y
 * aquí solo se traduce el choque a un 409 (criterios 1 y 2). La cotización noche
 * por noche y el anticipo requerido los resuelve servicio-recursos (criterios 4
 * y 5).
 */
@Service
public class GestionDeReservas {

    private static final String TIPO_CONSECUTIVO = "RESERVA";

    private final RepositorioDeReservas reservas;
    private final AsignadorDeConsecutivos consecutivos;
    private final CatalogoDeRecursos recursos;
    private final RegistroDeEventos eventos;

    public GestionDeReservas(RepositorioDeReservas reservas, AsignadorDeConsecutivos consecutivos,
            CatalogoDeRecursos recursos, RegistroDeEventos eventos) {
        this.reservas = reservas;
        this.consecutivos = consecutivos;
        this.recursos = recursos;
        this.eventos = eventos;
    }

    @Transactional
    @RequierePermiso("RESERVAS_RESERVA_CREAR")
    public ReservaDelNegocio crear(SolicitudDeReserva solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();

        if (solicitud.desde() == null || solicitud.hasta() == null
                || !solicitud.hasta().isAfter(solicitud.desde())) {
            throw new ReglaDeNegocioException(
                    "La salida de la reserva debe ser posterior a la entrada");
        }

        int noches = Reserva.nochesEntre(solicitud.desde(), solicitud.hasta());
        int adultos = solicitud.numAdultos() == null ? 1 : Math.max(1, solicitud.numAdultos());
        int ninos = solicitud.numNinos() == null ? 0 : Math.max(0, solicitud.numNinos());

        CotizacionDeEstadia cotizacion = recursos.cotizar(negocioId, solicitud.recursoId(),
                solicitud.desde().toLocalDate(), noches, adultos + ninos);
        AnticipoRequerido anticipo = recursos.anticipo(negocioId,
                solicitud.politicaCancelacionId(), cotizacion.total(), solicitud.desde());

        String numero = "RES-" + consecutivos.siguiente(negocioId, TIPO_CONSECUTIVO);
        Reserva reserva = Reserva.nueva(negocioId, numero, solicitud.tipoRecursoId(),
                solicitud.recursoId(), solicitud.desde(), solicitud.hasta(), adultos, ninos,
                CanalReserva.desde(solicitud.canal()), solicitud.clienteId(),
                solicitud.sucursalId(), cotizacion.tarifaDeCabecera(),
                anticipo.politicaCancelacionId(), cotizacion.total(), anticipo.anticipo(),
                cotizacion.moneda(), ContextoDeNegocio.usuarioActual(), solicitud.notas());

        try {
            reservas.insertar(reserva);
        } catch (DataIntegrityViolationException choca) {
            if (esSolapeDeRecurso(choca)) {
                throw new ConflictoDeEstadoException(
                        "El recurso ya está reservado en ese periodo");
            }
            throw choca;
        }

        publicarReservaCreada(reserva);
        return ReservaDelNegocio.de(reserva, cotizacion);
    }

    @Transactional(readOnly = true)
    @RequierePermiso("RESERVAS_RESERVA_VER")
    public ReservaDelNegocio ver(UUID reservaId) {
        return ReservaDelNegocio.de(reservas.buscar(reservaId)
                .filter(r -> r.getNegocioId().equals(ContextoDeNegocio.negocioActual()))
                .orElseThrow(() -> new NoEncontradoException("Esa reserva no existe")));
    }

    private static boolean esSolapeDeRecurso(DataIntegrityViolationException e) {
        String mensaje = e.getMostSpecificCause().getMessage();
        return mensaje != null && mensaje.contains(RepositorioDeReservas.CONSTRAINT_SOLAPE);
    }

    private void publicarReservaCreada(Reserva r) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("negocio_id", r.getNegocioId().toString());
        payload.put("reserva_id", r.getId().toString());
        payload.put("numero", r.getNumero());
        payload.put("tipo_recurso_id", r.getTipoRecursoId().toString());
        payload.put("recurso_id", r.getRecursoId().toString());
        payload.put("cliente_id", r.getClienteId() == null ? null : r.getClienteId().toString());
        payload.put("desde", r.getDesde().toString());
        payload.put("hasta", r.getHasta().toString());
        payload.put("noches", r.getNoches());
        payload.put("total", r.getTotal());
        payload.put("anticipo_requerido", r.getAnticipoRequerido());
        payload.put("saldo", r.getSaldo());
        payload.put("moneda", r.getMoneda());
        eventos.registrar(r.getNegocioId(), "Reserva", r.getId(), "reserva_creada", payload);
    }
}
