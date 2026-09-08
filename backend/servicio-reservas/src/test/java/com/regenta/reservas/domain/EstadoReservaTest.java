package com.regenta.reservas.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** HU-069 criterio 2. Solo PENDIENTE, CONFIRMADA y CHECK_IN ocupan el recurso. */
class EstadoReservaTest {

    @Test
    @DisplayName("Los estados que ocupan el recurso son exactamente los del EXCLUDE de la tabla")
    void estadosQueOcupan() {
        assertThat(EstadoReserva.PENDIENTE.ocupaRecurso()).isTrue();
        assertThat(EstadoReserva.CONFIRMADA.ocupaRecurso()).isTrue();
        assertThat(EstadoReserva.CHECK_IN.ocupaRecurso()).isTrue();

        assertThat(EstadoReserva.CHECK_OUT.ocupaRecurso()).isFalse();
        assertThat(EstadoReserva.CANCELADA.ocupaRecurso()).isFalse();
        assertThat(EstadoReserva.NO_SHOW.ocupaRecurso()).isFalse();
        assertThat(EstadoReserva.EXPIRADA.ocupaRecurso()).isFalse();
    }
}
