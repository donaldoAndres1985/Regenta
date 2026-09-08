package com.regenta.reservas.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** HU-069. Un tramo ocupado choca con un periodo consultado según el buffer del tipo. */
class VentanaOcupadaTest {

    private static final OffsetDateTime D1_15 = OffsetDateTime.parse("2026-07-01T15:00:00Z");
    private static final OffsetDateTime D2_11 = OffsetDateTime.parse("2026-07-02T11:00:00Z");

    private static VentanaOcupada ventana() {
        return new VentanaOcupada(UUID.randomUUID(), D1_15, D2_11);
    }

    @Test
    @DisplayName("Se solapa con un periodo que cae dentro")
    void solapeSimple() {
        assertThat(ventana().chocaCon(OffsetDateTime.parse("2026-07-02T08:00:00Z"),
                OffsetDateTime.parse("2026-07-02T18:00:00Z"), 0, 0)).isTrue();
    }

    @Test
    @DisplayName("Criterio 3: sin buffer, una consulta que arranca justo al check-out queda libre")
    void adyacenteSinBufferNoChoca() {
        assertThat(ventana().chocaCon(D2_11, OffsetDateTime.parse("2026-07-02T20:00:00Z"), 0, 0))
                .isFalse();
    }

    @Test
    @DisplayName("Criterio 4: con buffer de limpieza, el recurso sigue ocupado hasta que pasa el buffer")
    void bufferDeLimpiezaOcupaDespuesDelCheckout() {
        // 120 min de limpieza: ocupado hasta las 13:00.
        assertThat(ventana().chocaCon(D2_11, OffsetDateTime.parse("2026-07-02T20:00:00Z"), 0, 120))
                .isTrue();
        assertThat(ventana().chocaCon(OffsetDateTime.parse("2026-07-02T12:59:00Z"),
                OffsetDateTime.parse("2026-07-02T20:00:00Z"), 0, 120)).isTrue();
        assertThat(ventana().chocaCon(OffsetDateTime.parse("2026-07-02T13:00:00Z"),
                OffsetDateTime.parse("2026-07-02T20:00:00Z"), 0, 120)).isFalse();
    }

    @Test
    @DisplayName("El buffer de preparación adelanta la ocupación antes del check-in")
    void bufferDePreparacionOcupaAntesDelCheckin() {
        // 60 min de preparación: ocupado desde las 14:00.
        assertThat(ventana().chocaCon(OffsetDateTime.parse("2026-07-01T13:00:00Z"),
                OffsetDateTime.parse("2026-07-01T13:59:00Z"), 60, 0)).isFalse();
        assertThat(ventana().chocaCon(OffsetDateTime.parse("2026-07-01T13:00:00Z"),
                OffsetDateTime.parse("2026-07-01T14:30:00Z"), 60, 0)).isTrue();
    }
}
