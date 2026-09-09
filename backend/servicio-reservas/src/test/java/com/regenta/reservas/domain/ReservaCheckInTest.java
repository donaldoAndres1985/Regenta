package com.regenta.reservas.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;

/** HU-072. El check-in mueve la reserva a CHECK_IN y fija el recurso concreto. */
class ReservaCheckInTest {

    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-09-01T12:00:00Z");

    private static Reserva reserva(UUID recurso) {
        return Reserva.nueva(UUID.randomUUID(), "RES-1", UUID.randomUUID(), recurso,
                OffsetDateTime.parse("2026-10-01T15:00:00Z"),
                OffsetDateTime.parse("2026-10-03T11:00:00Z"), 2, 0, CanalReserva.MOSTRADOR, null,
                null, null, null, new BigDecimal("400000"), BigDecimal.ZERO, "COP", null, null);
    }

    @Test
    @DisplayName("Criterio 3: CONFIRMADA -> CHECK_IN con el recurso asignado")
    void checkInDesdeConfirmada() {
        UUID recurso = UUID.randomUUID();
        Reserva r = reserva(null).confirmar(AHORA).checkIn(recurso);

        assertThat(r.getEstado()).isEqualTo(EstadoReserva.CHECK_IN);
        assertThat(r.getRecursoId()).isEqualTo(recurso);
    }

    @Test
    @DisplayName("Si la reserva ya traía recurso y no se pasa otro, se conserva")
    void checkInConservaElRecursoPrevio() {
        UUID previo = UUID.randomUUID();
        Reserva r = reserva(previo).confirmar(AHORA).checkIn(null);
        assertThat(r.getRecursoId()).isEqualTo(previo);
    }

    @Test
    @DisplayName("Solo se hace check-in desde CONFIRMADA")
    void checkInSoloDesdeConfirmada() {
        assertThatThrownBy(() -> reserva(UUID.randomUUID()).checkIn(UUID.randomUUID()))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("El check-in sin recurso ni asignado previo se rechaza")
    void checkInSinRecurso() {
        assertThatThrownBy(() -> reserva(null).confirmar(AHORA).checkIn(null))
                .isInstanceOf(ReglaDeNegocioException.class);
    }
}
