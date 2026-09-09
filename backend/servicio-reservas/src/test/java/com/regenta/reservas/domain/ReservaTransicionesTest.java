package com.regenta.reservas.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.comun.errores.ConflictoDeEstadoException;

/** HU-071. Las transiciones de estado de una reserva. */
class ReservaTransicionesTest {

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-09-01T12:00:00Z");

    private static Reserva pendiente() {
        return Reserva.nueva(UUID.randomUUID(), "RES-1", UUID.randomUUID(), UUID.randomUUID(),
                OffsetDateTime.parse("2026-10-01T15:00:00Z"),
                OffsetDateTime.parse("2026-10-03T11:00:00Z"), 2, 0, CanalReserva.MOSTRADOR, null,
                null, UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("500000"),
                new BigDecimal("150000"), "COP", null, null);
    }

    @Test
    @DisplayName("Criterio 1: PENDIENTE -> CONFIRMADA deja la fecha de confirmación")
    void confirmar() {
        Reserva r = pendiente().confirmar(AHORA);

        assertThat(r.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(r.getConfirmadaEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Solo se confirma una reserva pendiente")
    void confirmarSoloDesdePendiente() {
        Reserva confirmada = pendiente().confirmar(AHORA);
        assertThatThrownBy(() -> confirmada.confirmar(AHORA))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("Criterios 2 y 3: cancelar fija la penalización que calculó la política, acotada al total")
    void cancelarFijaPenalizacion() {
        Reserva sinCosto = pendiente().cancelar(BigDecimal.ZERO, "cambio de planes", AHORA);
        assertThat(sinCosto.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(sinCosto.getPenalizacion()).isEqualByComparingTo("0");
        assertThat(sinCosto.getMotivoCancelacion()).isEqualTo("cambio de planes");
        assertThat(sinCosto.getCanceladaEn()).isEqualTo(AHORA);

        Reserva conCosto = pendiente().cancelar(new BigDecimal("250000"), null, AHORA);
        assertThat(conCosto.getPenalizacion()).isEqualByComparingTo("250000");

        Reserva topada = pendiente().cancelar(new BigDecimal("999999999"), null, AHORA);
        assertThat(topada.getPenalizacion()).isEqualByComparingTo("500000");
    }

    @Test
    @DisplayName("No se cancela una reserva ya cancelada")
    void noSeCancelaDosVeces() {
        Reserva cancelada = pendiente().cancelar(BigDecimal.ZERO, null, AHORA);
        assertThatThrownBy(() -> cancelada.cancelar(BigDecimal.ZERO, null, AHORA))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("Solo una reserva confirmada puede marcarse no-show")
    void noShowSoloDesdeConfirmada() {
        assertThatThrownBy(() -> pendiente().marcarNoShow(AHORA))
                .isInstanceOf(ConflictoDeEstadoException.class);

        Reserva noShow = pendiente().confirmar(AHORA).marcarNoShow(AHORA);
        assertThat(noShow.getEstado()).isEqualTo(EstadoReserva.NO_SHOW);
    }

    @Test
    @DisplayName("CANCELADA y NO_SHOW no ocupan el recurso")
    void estadosQueLiberan() {
        assertThat(EstadoReserva.CANCELADA.ocupaRecurso()).isFalse();
        assertThat(EstadoReserva.NO_SHOW.ocupaRecurso()).isFalse();
    }

    @Test
    @DisplayName("horasHastaLaEntrada mide la antelación con la que se cancela")
    void horasHastaLaEntrada() {
        Reserva r = pendiente();
        long horas = r.horasHastaLaEntrada(OffsetDateTime.parse("2026-09-29T15:00:00Z")
                .withOffsetSameInstant(ZoneOffset.UTC));
        assertThat(horas).isEqualTo(48);
    }
}
