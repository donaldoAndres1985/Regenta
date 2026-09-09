package com.regenta.reservas.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.comun.errores.ConflictoDeEstadoException;

/** HU-074. El check-out cierra la estancia y pasa la reserva a CHECK_OUT. */
class ReservaCheckOutTest {

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-09-01T12:00:00Z");

    private static Reserva confirmadaConCheckIn() {
        return Reserva.nueva(UUID.randomUUID(), "RES-1", UUID.randomUUID(), UUID.randomUUID(),
                OffsetDateTime.parse("2026-10-01T15:00:00Z"),
                OffsetDateTime.parse("2026-10-03T11:00:00Z"), 2, 0, CanalReserva.MOSTRADOR, null,
                null, null, null, new BigDecimal("400000"), BigDecimal.ZERO, "COP", null, null)
                .confirmar(AHORA).checkIn(UUID.randomUUID());
    }

    @Test
    @DisplayName("CHECK_IN -> CHECK_OUT")
    void checkOutDesdeCheckIn() {
        assertThat(confirmadaConCheckIn().checkOut().getEstado()).isEqualTo(EstadoReserva.CHECK_OUT);
    }

    @Test
    @DisplayName("Solo se hace check-out de una reserva con check-in hecho")
    void checkOutSoloDesdeCheckIn() {
        Reserva confirmada = Reserva.nueva(UUID.randomUUID(), "RES-2", UUID.randomUUID(),
                UUID.randomUUID(), OffsetDateTime.parse("2026-10-01T15:00:00Z"),
                OffsetDateTime.parse("2026-10-03T11:00:00Z"), 1, 0, CanalReserva.MOSTRADOR, null,
                null, null, null, new BigDecimal("100"), BigDecimal.ZERO, "COP", null, null)
                .confirmar(AHORA);
        assertThatThrownBy(confirmada::checkOut).isInstanceOf(ConflictoDeEstadoException.class);

        Reserva yaSalida = confirmadaConCheckIn().checkOut();
        assertThatThrownBy(yaSalida::checkOut).isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("Estancia.cerrar: EN_CURSO -> FINALIZADA con la fecha de salida")
    void cerrarEstancia() {
        Estancia estancia = Estancia.abrir(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                AHORA, UUID.randomUUID(), AHORA.plusDays(2), BigDecimal.ZERO, null);
        UUID usuario = UUID.randomUUID();

        Estancia cerrada = estancia.cerrar(AHORA.plusDays(2), usuario);
        assertThat(cerrada.getEstado()).isEqualTo(EstadoEstancia.FINALIZADA);
        assertThat(cerrada.getCheckOutEn()).isEqualTo(AHORA.plusDays(2));
        assertThat(cerrada.getCheckOutUsuarioId()).isEqualTo(usuario);

        assertThatThrownBy(() -> cerrada.cerrar(AHORA.plusDays(3), usuario))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }
}
