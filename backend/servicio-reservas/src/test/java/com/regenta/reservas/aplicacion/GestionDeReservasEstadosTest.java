package com.regenta.reservas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.reservas.BaseDeReservas;
import com.regenta.reservas.infra.CatalogoDeRecursosStub;

/**
 * HU-071. Confirmar, cancelar y marcar no-show, con dos negocios cargados. Cada
 * transición deja rastro en {@code reserva_eventos} y publica su evento.
 */
class GestionDeReservasEstadosTest extends BaseDeReservas {

    private static final java.util.Set<String> RECEPCION = java.util.Set.of(
            "RESERVAS_RESERVA_VER", "RESERVAS_RESERVA_CREAR", "RESERVAS_RESERVA_EDITAR",
            "RESERVAS_RESERVA_ANULAR");

    @Autowired
    private GestionDeReservas reservas;
    @Autowired
    private CatalogoDeRecursosStub catalogo;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID recepcion = UUID.randomUUID();
    private final UUID tipo = UUID.randomUUID();

    @BeforeEach
    void limpiar() {
        catalogo.reiniciar();
    }

    private OffsetDateTime enDias(int dias) {
        return OffsetDateTime.now(ZoneOffset.UTC).plusDays(dias).withNano(0);
    }

    private OffsetDateTime enHoras(int horas) {
        return OffsetDateTime.now(ZoneOffset.UTC).plusHours(horas).withNano(0);
    }

    private SolicitudDeReserva solicitud(UUID recursoId, OffsetDateTime desde, OffsetDateTime hasta) {
        return new SolicitudDeReserva(desde, hasta, tipo, recursoId, null, null, 2, 0, null,
                "MOSTRADOR", null);
    }

    private ReservaDelNegocio crear(UUID negocio, SolicitudDeReserva s) {
        return enContexto(negocio, recepcion, RECEPCION, () -> reservas.crear(s));
    }

    private void pagar(UUID negocio, UUID reservaId, String monto) {
        enContexto(negocio, recepcion, RECEPCION, () -> reservas.registrarPago(reservaId,
                new SolicitudDePagoDeReserva("ANTICIPO", "EFECTIVO", new BigDecimal(monto), null,
                        null)));
    }

    private long eventosDe(UUID reservaId) {
        return contar("SELECT count(*) FROM reserva_eventos WHERE reserva_id = '" + reservaId + "'");
    }

    @Test
    @DisplayName("Criterio 1: con el anticipo cobrado, confirmar pasa a CONFIRMADA y publica reserva_confirmada")
    void confirmarConAnticipo() {
        catalogo.anticipoPct(new BigDecimal("0.30"), UUID.randomUUID());
        UUID recurso = UUID.randomUUID();
        ReservaDelNegocio r = crear(negocioA, solicitud(recurso, enDias(20), enDias(22)));
        assertThat(r.anticipoRequerido()).isEqualByComparingTo("60000");

        assertThatThrownBy(() -> enContexto(negocioA, recepcion, RECEPCION,
                () -> reservas.confirmar(r.id()))).isInstanceOf(ConflictoDeEstadoException.class);

        pagar(negocioA, r.id(), "60000");
        ReservaDelNegocio confirmada = enContexto(negocioA, recepcion, RECEPCION,
                () -> reservas.confirmar(r.id()));

        assertThat(confirmada.estado()).isEqualTo("CONFIRMADA");
        assertThat(confirmada.confirmadaEn()).isNotNull();
        assertThat(contar("SELECT count(*) FROM outbox_eventos WHERE agregado_id = '" + r.id()
                + "' AND tipo_evento = 'reserva_confirmada'")).isEqualTo(1);
    }

    @Test
    @DisplayName("Criterio 2: cancelar dentro del plazo de la política no penaliza")
    void cancelarDentroDePlazo() {
        UUID politica = UUID.randomUUID();
        catalogo.politicaDeCancelacion(new BigDecimal("0.50"), 48, politica);
        UUID recurso = UUID.randomUUID();
        ReservaDelNegocio r = crear(negocioA, solicitud(recurso, enDias(30), enDias(32)));

        ReservaDelNegocio cancelada = enContexto(negocioA, recepcion, RECEPCION,
                () -> reservas.cancelar(r.id(), new SolicitudDeCancelacion("el cliente canceló")));

        assertThat(cancelada.estado()).isEqualTo("CANCELADA");
        assertThat(cancelada.penalizacion()).isEqualByComparingTo("0");
        assertThat(cancelada.canceladaEn()).isNotNull();
    }

    @Test
    @DisplayName("Criterio 3: cancelar fuera de plazo penaliza sobre el total")
    void cancelarFueraDePlazo() {
        UUID politica = UUID.randomUUID();
        catalogo.politicaDeCancelacion(new BigDecimal("0.50"), 48, politica);
        UUID recurso = UUID.randomUUID();
        // Entra en 12 h: menos de las 48 h que pide la política.
        ReservaDelNegocio r = crear(negocioA, solicitud(recurso, enHoras(12), enDias(2)));

        ReservaDelNegocio cancelada = enContexto(negocioA, recepcion, RECEPCION,
                () -> reservas.cancelar(r.id(), null));

        // total = 100000 * noches; 1 o 2 noches según el redondeo de fechas, penalización = 50 %.
        assertThat(cancelada.penalizacion()).isEqualByComparingTo(
                r.total().multiply(new BigDecimal("0.50")));
        assertThat(cancelada.penalizacion().signum()).isPositive();
    }

    @Test
    @DisplayName("Criterio 4: cancelada, el recurso queda libre de inmediato sin borrar el registro")
    void canceladaLiberaElRecurso() {
        UUID recurso = UUID.randomUUID();
        ReservaDelNegocio a = crear(negocioA, solicitud(recurso, enDias(10), enDias(13)));

        // Mientras A vive, otra que se solapa choca.
        assertThatThrownBy(() -> crear(negocioA, solicitud(recurso, enDias(11), enDias(14))))
                .isInstanceOf(ConflictoDeEstadoException.class);

        enContexto(negocioA, recepcion, RECEPCION, () -> reservas.cancelar(a.id(), null));

        // El registro sigue existiendo...
        assertThat(enContexto(negocioA, recepcion, RECEPCION, () -> reservas.ver(a.id())).estado())
                .isEqualTo("CANCELADA");
        // ...y el recurso ya se puede volver a reservar en ese periodo.
        assertThatCode(() -> crear(negocioA, solicitud(recurso, enDias(11), enDias(14))))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Criterio 5: cada cambio de estado queda en reserva_eventos con autor y fecha")
    void cadaCambioDejaEvento() {
        catalogo.anticipoPct(BigDecimal.ZERO, UUID.randomUUID());
        UUID recurso = UUID.randomUUID();
        ReservaDelNegocio r = crear(negocioA, solicitud(recurso, enDias(15), enDias(17)));

        assertThat(eventosDe(r.id())).isEqualTo(1); // creación

        enContexto(negocioA, recepcion, RECEPCION, () -> reservas.confirmar(r.id()));
        enContexto(negocioA, recepcion, RECEPCION, () -> reservas.marcarNoShow(r.id()));

        assertThat(eventosDe(r.id())).isEqualTo(3);
        assertThat(consultar("SELECT estado_nuevo FROM reserva_eventos WHERE reserva_id = '"
                + r.id() + "' ORDER BY ocurrido_en"))
                .containsExactly("PENDIENTE", "CONFIRMADA", "NO_SHOW");
        assertThat(contar("SELECT count(*) FROM reserva_eventos WHERE reserva_id = '" + r.id()
                + "' AND usuario_id = '" + recepcion + "'")).isEqualTo(3);
    }

    @Test
    @DisplayName("No-show solo desde CONFIRMADA; confirmar dos veces o cancelar una cancelada dan 409")
    void transicionesInvalidas() {
        catalogo.anticipoPct(BigDecimal.ZERO, UUID.randomUUID());
        UUID recurso = UUID.randomUUID();
        ReservaDelNegocio r = crear(negocioA, solicitud(recurso, enDias(18), enDias(20)));

        assertThatThrownBy(() -> enContexto(negocioA, recepcion, RECEPCION,
                () -> reservas.marcarNoShow(r.id()))).isInstanceOf(ConflictoDeEstadoException.class);

        enContexto(negocioA, recepcion, RECEPCION, () -> reservas.confirmar(r.id()));
        assertThatThrownBy(() -> enContexto(negocioA, recepcion, RECEPCION,
                () -> reservas.confirmar(r.id()))).isInstanceOf(ConflictoDeEstadoException.class);

        enContexto(negocioA, recepcion, RECEPCION, () -> reservas.cancelar(r.id(), null));
        assertThatThrownBy(() -> enContexto(negocioA, recepcion, RECEPCION,
                () -> reservas.cancelar(r.id(), null))).isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("El segundo negocio no puede confirmar ni cancelar la reserva del primero")
    void aislamientoEntreNegocios() {
        catalogo.anticipoPct(BigDecimal.ZERO, UUID.randomUUID());
        UUID enA = crear(negocioA, solicitud(UUID.randomUUID(), enDias(5), enDias(7))).id();

        assertThatThrownBy(() -> enContexto(negocioB, recepcion, RECEPCION,
                () -> reservas.confirmar(enA))).isInstanceOf(NoEncontradoException.class);
        assertThatThrownBy(() -> enContexto(negocioB, recepcion, RECEPCION,
                () -> reservas.cancelar(enA, null))).isInstanceOf(NoEncontradoException.class);
    }
}
