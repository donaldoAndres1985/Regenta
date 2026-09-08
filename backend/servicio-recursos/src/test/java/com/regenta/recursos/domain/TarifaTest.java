package com.regenta.recursos.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * HU-066. Reglas puras de una tarifa: cuándo aplica a una noche y cuánto cobra.
 * No tocan la base; la cotización noche por noche se prueba en
 * {@code CotizadorDeEstadiaTest}.
 */
class TarifaTest {

    private static final UUID NEGOCIO = UUID.randomUUID();
    private static final UUID TIPO = UUID.randomUUID();

    private static Tarifa tarifa(LocalDate desde, LocalDate hasta, List<Integer> dias,
            int estanciaMinima, int prioridad) {
        return Tarifa.crear(NEGOCIO, TIPO, null, "Tarifa", UnidadTiempo.NOCHE,
                new BigDecimal("100000.0000"), new BigDecimal("20000.0000"),
                desde, hasta, dias, null, null, estanciaMinima, prioridad);
    }

    @Test
    @DisplayName("Criterio 2: una noche fuera del rango de vigencia no aplica")
    void nocheFueraDeVigenciaNoAplica() {
        Tarifa temporada = tarifa(LocalDate.of(2026, 12, 24), LocalDate.of(2026, 12, 26),
                null, 1, 0);

        assertThat(temporada.aplicaEn(LocalDate.of(2026, 12, 23), 1)).isFalse();
        assertThat(temporada.aplicaEn(LocalDate.of(2026, 12, 27), 1)).isFalse();
        assertThat(temporada.aplicaEn(LocalDate.of(2026, 12, 24), 1)).isTrue();
        assertThat(temporada.aplicaEn(LocalDate.of(2026, 12, 26), 1)).isTrue();
    }

    @Test
    @DisplayName("Criterio 3: una tarifa de viernes a domingo no aplica un martes")
    void tarifaDeFinDeSemanaNoAplicaEntreSemana() {
        // 1=lunes .. 7=domingo
        Tarifa finDeSemana = tarifa(null, null, List.of(5, 6, 7), 1, 0);

        LocalDate martes = LocalDate.of(2026, 5, 12);
        LocalDate sabado = LocalDate.of(2026, 5, 16);
        assertThat(martes.getDayOfWeek().getValue()).isEqualTo(2);
        assertThat(sabado.getDayOfWeek().getValue()).isEqualTo(6);

        assertThat(finDeSemana.aplicaEn(martes, 1)).isFalse();
        assertThat(finDeSemana.aplicaEn(sabado, 1)).isTrue();
    }

    @Test
    @DisplayName("Criterio 5: una tarifa con estancia mínima de dos noches no aplica a una sola")
    void estanciaMinimaNoAplicaAUnaNoche() {
        Tarifa dosNoches = tarifa(null, null, null, 2, 0);

        assertThat(dosNoches.aplicaEn(LocalDate.of(2026, 5, 16), 1)).isFalse();
        assertThat(dosNoches.aplicaEn(LocalDate.of(2026, 5, 16), 2)).isTrue();
        assertThat(dosNoches.aplicaEn(LocalDate.of(2026, 5, 16), 3)).isTrue();
    }

    @Test
    @DisplayName("Una tarifa desactivada no aplica a ninguna noche")
    void desactivadaNoAplica() {
        Tarifa t = tarifa(null, null, null, 1, 0);
        t.desactivar();

        assertThat(t.aplicaEn(LocalDate.of(2026, 5, 16), 1)).isFalse();
    }

    @Test
    @DisplayName("El precio de una noche suma la persona adicional a partir de la segunda")
    void precioDeNocheConPersonaAdicional() {
        Tarifa t = tarifa(null, null, null, 1, 0);

        assertThat(t.precioDeNoche(1)).isEqualByComparingTo("100000");
        assertThat(t.precioDeNoche(2)).isEqualByComparingTo("120000");
        assertThat(t.precioDeNoche(4)).isEqualByComparingTo("160000");
    }

    @Test
    @DisplayName("Sin días de la semana, la tarifa aplica todos los días")
    void sinDiasAplicaTodosLosDias() {
        Tarifa t = tarifa(null, null, null, 1, 0);

        for (int i = 0; i < 7; i++) {
            assertThat(t.aplicaEn(LocalDate.of(2026, 5, 11).plusDays(i), 1)).isTrue();
        }
        assertThat(t.getDiasSemana()).containsExactly(1, 2, 3, 4, 5, 6, 7);
    }
}
