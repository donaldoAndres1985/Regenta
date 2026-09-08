package com.regenta.recursos.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** HU-064: la ventana con buffer, sin Spring. */
class TipoDeRecursoTest {

    @Test
    @DisplayName("Criterio 3: los buffers de preparación y limpieza expanden la ventana bloqueada")
    void ventanaConBuffer() {
        TipoDeRecurso habitacion = TipoDeRecurso.crear(java.util.UUID.randomUUID(),
                "Habitación doble", null, UnidadTiempo.NOCHE, 1440, 1440, 2, false, 15, 30);

        OffsetDateTime inicio = OffsetDateTime.parse("2026-09-10T14:00:00Z");
        OffsetDateTime fin = OffsetDateTime.parse("2026-09-11T12:00:00Z");
        OffsetDateTime[] ventana = habitacion.ventanaConBuffer(inicio, fin);

        assertThat(ventana[0]).isEqualTo(OffsetDateTime.parse("2026-09-10T13:45:00Z"));
        assertThat(ventana[1]).isEqualTo(OffsetDateTime.parse("2026-09-11T12:30:00Z"));
    }

    @Test
    @DisplayName("Sin buffers, la ventana es exactamente la reservada")
    void sinBuffer() {
        TipoDeRecurso cancha = TipoDeRecurso.crear(java.util.UUID.randomUUID(), "Cancha F5", null,
                UnidadTiempo.HORA, 60, 30, 10, false, 0, 0);

        OffsetDateTime inicio = OffsetDateTime.parse("2026-09-10T18:00:00Z");
        OffsetDateTime fin = OffsetDateTime.parse("2026-09-10T19:00:00Z");
        OffsetDateTime[] ventana = cancha.ventanaConBuffer(inicio, fin);

        assertThat(ventana[0]).isEqualTo(inicio);
        assertThat(ventana[1]).isEqualTo(fin);
    }
}
