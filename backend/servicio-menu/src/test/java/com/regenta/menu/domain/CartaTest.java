package com.regenta.menu.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** HU-076 criterio 1. Una carta solo está disponible dentro de su franja, sus días y su vigencia. */
class CartaTest {

    private static final UUID NEG = UUID.randomUUID();

    private static Carta carta(LocalTime desde, LocalTime hasta, List<Integer> dias,
            LocalDate vigDesde, LocalDate vigHasta) {
        return Carta.crear(NEG, null, "Carta", null, desde, hasta, dias, vigDesde, vigHasta, false);
    }

    @Test
    @DisplayName("Una carta de 6:00 a 11:00 no aparece a las 15:00")
    void fueraDeLaFranjaHoraria() {
        Carta desayunos = carta(LocalTime.of(6, 0), LocalTime.of(11, 0), null, null, null);

        assertThat(desayunos.disponibleEn(LocalDateTime.of(2026, 5, 4, 15, 0))).isFalse();
        assertThat(desayunos.disponibleEn(LocalDateTime.of(2026, 5, 4, 8, 0))).isTrue();
        assertThat(desayunos.disponibleEn(LocalDateTime.of(2026, 5, 4, 11, 0))).isTrue();
    }

    @Test
    @DisplayName("Una franja que cruza la medianoche (22:00–02:00) se entiende bien")
    void franjaQueCruzaMedianoche() {
        Carta happyHour = carta(LocalTime.of(22, 0), LocalTime.of(2, 0), null, null, null);

        assertThat(happyHour.disponibleEn(LocalDateTime.of(2026, 5, 4, 23, 30))).isTrue();
        assertThat(happyHour.disponibleEn(LocalDateTime.of(2026, 5, 4, 1, 0))).isTrue();
        assertThat(happyHour.disponibleEn(LocalDateTime.of(2026, 5, 4, 12, 0))).isFalse();
    }

    @Test
    @DisplayName("Fuera del rango de vigencia de fechas no aparece")
    void fueraDeVigencia() {
        Carta temporada = carta(null, null, null, LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 31));

        assertThat(temporada.disponibleEn(LocalDateTime.of(2026, 11, 30, 12, 0))).isFalse();
        assertThat(temporada.disponibleEn(LocalDateTime.of(2027, 1, 1, 12, 0))).isFalse();
        assertThat(temporada.disponibleEn(LocalDateTime.of(2026, 12, 15, 12, 0))).isTrue();
    }

    @Test
    @DisplayName("Un día de la semana no permitido descarta la carta")
    void diaNoPermitido() {
        // Solo fines de semana (6=sábado, 7=domingo).
        Carta brunch = carta(null, null, List.of(6, 7), null, null);
        // 2026-05-04 es lunes; 2026-05-09 es sábado.
        assertThat(brunch.disponibleEn(LocalDateTime.of(2026, 5, 4, 12, 0))).isFalse();
        assertThat(brunch.disponibleEn(LocalDateTime.of(2026, 5, 9, 12, 0))).isTrue();
    }

    @Test
    @DisplayName("Una carta sin franja ni días definidos vale siempre; inactiva, nunca")
    void sinFranjaOInactiva() {
        assertThat(carta(null, null, null, null, null)
                .disponibleEn(LocalDateTime.of(2026, 5, 4, 3, 0))).isTrue();

        Carta apagada = carta(null, null, null, null, null);
        apagada.desactivar();
        assertThat(apagada.disponibleEn(LocalDateTime.of(2026, 5, 4, 12, 0))).isFalse();
        assertThat(apagada.getDiasSemana()).containsExactly(1, 2, 3, 4, 5, 6, 7);
    }
}
