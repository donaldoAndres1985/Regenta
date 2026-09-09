package com.regenta.reservas.aplicacion;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
import com.regenta.reservas.domain.EstadoReserva;
import com.regenta.reservas.domain.MetodoDePago;
import com.regenta.reservas.domain.PagoDeReserva;
import com.regenta.reservas.domain.Reserva;
import com.regenta.reservas.domain.TipoDePagoReserva;
import com.regenta.reservas.infra.AsignadorDeConsecutivos;
import com.regenta.reservas.infra.RepositorioDePagosDeReserva;
import com.regenta.reservas.infra.RepositorioDeReservas;

/**
 * El ciclo de vida de una reserva: crearla sin overbooking (HU-070) y moverla
 * por sus estados (HU-071). El anti-overbooking lo garantiza el
 * {@code EXCLUDE USING gist} de la tabla, no un chequeo en Java; aquí solo se
 * traduce el choque a un 409. La cotización, el anticipo y la penalización los
 * resuelve servicio-recursos por el puerto {@link CatalogoDeRecursos}.
 */
@Service
public class GestionDeReservas {

    private static final String TIPO_CONSECUTIVO = "RESERVA";

    private final RepositorioDeReservas reservas;
    private final RepositorioDePagosDeReserva pagos;
    private final AsignadorDeConsecutivos consecutivos;
    private final CatalogoDeRecursos recursos;
    private final RegistroDeEventos eventos;

    public GestionDeReservas(RepositorioDeReservas reservas, RepositorioDePagosDeReserva pagos,
            AsignadorDeConsecutivos consecutivos, CatalogoDeRecursos recursos,
            RegistroDeEventos eventos) {
        this.reservas = reservas;
        this.pagos = pagos;
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

        reservas.registrarEvento(negocioId, reserva.getId(), null, EstadoReserva.PENDIENTE,
                ContextoDeNegocio.usuarioActual(), "Reserva creada");
        publicar(reserva, "reserva_creada");
        return ReservaDelNegocio.de(reserva, BigDecimal.ZERO, cotizacion);
    }

    @Transactional
    @RequierePermiso("RESERVAS_RESERVA_EDITAR")
    public ReservaDelNegocio registrarPago(UUID reservaId, SolicitudDePagoDeReserva solicitud) {
        Reserva reserva = delNegocio(reservaId);
        PagoDeReserva pago = PagoDeReserva.nuevo(reserva.getNegocioId(), reserva.getId(),
                TipoDePagoReserva.desde(solicitud.tipo()), MetodoDePago.desde(solicitud.metodo()),
                solicitud.monto(), solicitud.referencia(), solicitud.cajaSesionId(),
                ContextoDeNegocio.usuarioActual());
        pagos.registrar(pago);
        return ReservaDelNegocio.de(reserva, pagos.abonadoA(reserva.getId()), null);
    }

    @Transactional
    @RequierePermiso("RESERVAS_RESERVA_EDITAR")
    public ReservaDelNegocio confirmar(UUID reservaId) {
        Reserva reserva = delNegocio(reservaId);
        BigDecimal abonado = pagos.abonadoA(reserva.getId());
        if (abonado.compareTo(reserva.getAnticipoRequerido()) < 0) {
            throw new ConflictoDeEstadoException(
                    "Falta cobrar el anticipo antes de confirmar (van " + abonado + " de "
                            + reserva.getAnticipoRequerido() + ")");
        }
        Reserva confirmada = reserva.confirmar(OffsetDateTime.now(ZoneOffset.UTC));
        guardarTransicion(reserva, confirmada, "Confirmada");
        publicar(confirmada, "reserva_confirmada");
        return ReservaDelNegocio.de(confirmada, abonado, null);
    }

    @Transactional
    @RequierePermiso("RESERVAS_RESERVA_ANULAR")
    public ReservaDelNegocio cancelar(UUID reservaId, SolicitudDeCancelacion solicitud) {
        Reserva reserva = delNegocio(reservaId);
        PenalizacionDeCancelacion calculo = recursos.penalizacionPorCancelar(
                reserva.getNegocioId(), reserva.getPoliticaCancelacionId(), reserva.getTotal(),
                reserva.getDesde());
        String motivo = solicitud == null ? null : solicitud.motivo();
        Reserva cancelada = reserva.cancelar(calculo.penalizacion(), motivo,
                OffsetDateTime.now(ZoneOffset.UTC));
        guardarTransicion(reserva, cancelada,
                "Cancelada" + (motivo == null ? "" : ": " + motivo)
                        + " (penalización " + cancelada.getPenalizacion() + ", "
                        + (calculo.dentroDePlazo() ? "dentro de plazo" : "fuera de plazo") + ")");
        publicar(cancelada, "reserva_cancelada");
        return ReservaDelNegocio.de(cancelada, pagos.abonadoA(reserva.getId()), null);
    }

    @Transactional
    @RequierePermiso("RESERVAS_RESERVA_EDITAR")
    public ReservaDelNegocio marcarNoShow(UUID reservaId) {
        Reserva reserva = delNegocio(reservaId);
        Reserva noShow = reserva.marcarNoShow(OffsetDateTime.now(ZoneOffset.UTC));
        guardarTransicion(reserva, noShow, "No-show");
        publicar(noShow, "reserva_no_show");
        return ReservaDelNegocio.de(noShow, pagos.abonadoA(reserva.getId()), null);
    }

    @Transactional(readOnly = true)
    @RequierePermiso("RESERVAS_RESERVA_VER")
    public ReservaDelNegocio ver(UUID reservaId) {
        Reserva reserva = delNegocio(reservaId);
        return ReservaDelNegocio.de(reserva, pagos.abonadoA(reserva.getId()), null);
    }

    private void guardarTransicion(Reserva antes, Reserva despues, String detalle) {
        if (!reservas.actualizarEstado(despues, antes.getVersion())) {
            throw new ConflictoDeEstadoException(
                    "La reserva cambió mientras se procesaba; volvé a intentarlo");
        }
        reservas.registrarEvento(antes.getNegocioId(), antes.getId(), antes.getEstado(),
                despues.getEstado(), ContextoDeNegocio.usuarioActual(), detalle);
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

    private void publicar(Reserva r, String tipoEvento) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("negocio_id", r.getNegocioId().toString());
        payload.put("reserva_id", r.getId().toString());
        payload.put("numero", r.getNumero());
        payload.put("estado", r.getEstado().name());
        payload.put("tipo_recurso_id", r.getTipoRecursoId().toString());
        payload.put("recurso_id", r.getRecursoId() == null ? null : r.getRecursoId().toString());
        payload.put("cliente_id", r.getClienteId() == null ? null : r.getClienteId().toString());
        payload.put("desde", r.getDesde().toString());
        payload.put("hasta", r.getHasta().toString());
        payload.put("noches", r.getNoches());
        payload.put("total", r.getTotal());
        payload.put("anticipo_requerido", r.getAnticipoRequerido());
        payload.put("saldo", r.getSaldo());
        payload.put("penalizacion", r.getPenalizacion());
        payload.put("moneda", r.getMoneda());
        eventos.registrar(r.getNegocioId(), "Reserva", r.getId(), tipoEvento, payload);
    }
}
