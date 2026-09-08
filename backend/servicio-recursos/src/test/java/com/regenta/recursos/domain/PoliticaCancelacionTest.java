package com.regenta.recursos.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.comun.errores.ReglaDeNegocioException;

/** HU-068 criterio 3. Una política de cancelación define anticipo requerido y penalización. */
class PoliticaCancelacionTest {

    private static final UUID NEGOCIO = UUID.randomUUID();
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-06-01T12:00:00Z");

    private static PoliticaCancelacion politica(int horasAntes, String penalPct, String anticipoPct) {
        return PoliticaCancelacion.crear(NEGOCIO, "Estándar", horasAntes, new BigDecimal(penalPct),
                new BigDecimal(anticipoPct), false);
    }

    @Test
    @DisplayName("Cancelar con la antelación suficiente: sin penalización, pero el anticipo se calcula igual")
    void dentroDePlazoSinPenalizacion() {
        PoliticaCancelacion p = politica(24, "0.5", "0.3");
        // Entrada 48 h después de la cancelación → dentro de plazo.
        ResultadoDeCancelacion r = p.aplicarA(new BigDecimal("200000"), AHORA, AHORA.plusHours(48));

        assertThat(r.dentroDePlazo()).isTrue();
        assertThat(r.horasDeAntelacion()).isEqualTo(48);
        assertThat(r.penalizacion()).isEqualByComparingTo("0");
        assertThat(r.anticipoRequerido()).isEqualByComparingTo("60000");
    }

    @Test
    @DisplayName("Cancelar tarde: se retiene la penalización sobre el monto de la reserva")
    void fueraDePlazoConPenalizacion() {
        PoliticaCancelacion p = politica(24, "0.5", "0.3");
        // Entrada 6 h después → fuera de plazo.
        ResultadoDeCancelacion r = p.aplicarA(new BigDecimal("200000"), AHORA, AHORA.plusHours(6));

        assertThat(r.dentroDePlazo()).isFalse();
        assertThat(r.penalizacion()).isEqualByComparingTo("100000");
        assertThat(r.anticipoRequerido()).isEqualByComparingTo("60000");
    }

    @Test
    @DisplayName("El límite es inclusivo: exactamente horasAntes de antelación no penaliza")
    void limiteInclusivo() {
        PoliticaCancelacion p = politica(24, "0.4", "0");
        ResultadoDeCancelacion r = p.aplicarA(new BigDecimal("100000"), AHORA, AHORA.plusHours(24));

        assertThat(r.dentroDePlazo()).isTrue();
        assertThat(r.penalizacion()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Un porcentaje fuera de 0..1 o unas horas negativas se rechazan")
    void valoresInvalidos() {
        assertThatThrownBy(() -> politica(24, "1.5", "0"))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> politica(24, "0", "-0.1"))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> politica(-1, "0", "0"))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Sin monto de reserva, todo queda en cero")
    void sinMonto() {
        PoliticaCancelacion p = politica(24, "0.5", "0.3");
        ResultadoDeCancelacion r = p.aplicarA(null, AHORA, AHORA.plusHours(1));

        assertThat(r.penalizacion()).isEqualByComparingTo("0");
        assertThat(r.anticipoRequerido()).isEqualByComparingTo("0");
    }
}
