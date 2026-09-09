package com.regenta.reservas.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.comun.errores.ReglaDeNegocioException;

/** HU-070. Reglas puras de una reserva: el periodo y el desglose de dinero. */
class ReservaTest {

    private static final UUID NEG = UUID.randomUUID();
    private static final UUID TIPO = UUID.randomUUID();
    private static final UUID RECURSO = UUID.randomUUID();
    private static final OffsetDateTime D1_15 = OffsetDateTime.parse("2026-07-01T15:00:00Z");
    private static final OffsetDateTime D3_11 = OffsetDateTime.parse("2026-07-03T11:00:00Z");

    private static Reserva nueva(OffsetDateTime desde, OffsetDateTime hasta, UUID recurso,
            String total, String anticipo) {
        return Reserva.nueva(NEG, "RES-1", TIPO, recurso, desde, hasta, 2, 0,
                CanalReserva.MOSTRADOR, null, null, UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal(total), new BigDecimal(anticipo), "COP", null, null);
    }

    @Test
    @DisplayName("Las noches se cuentan por fecha, con mínimo 1")
    void nochesPorFecha() {
        assertThat(Reserva.nochesEntre(D1_15, D3_11)).isEqualTo(2);
        assertThat(Reserva.nochesEntre(D1_15, OffsetDateTime.parse("2026-07-02T11:00:00Z")))
                .isEqualTo(1);
        assertThat(Reserva.nochesEntre(D1_15, OffsetDateTime.parse("2026-07-01T23:00:00Z")))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Criterio 6: un periodo con fin anterior o igual al inicio se rechaza")
    void periodoInvalido() {
        assertThatThrownBy(() -> nueva(D3_11, D1_15, RECURSO, "100", "0"))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> nueva(D1_15, D1_15, RECURSO, "100", "0"))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatCode(() -> nueva(D1_15, D3_11, RECURSO, "100", "0")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Una reserva puede nacer sin recurso: se vende por tipo y se asigna en el check-in")
    void sinRecursoSeVendePorTipo() {
        Reserva r = nueva(D1_15, D3_11, null, "100000", "0");
        assertThat(r.getRecursoId()).isNull();
        assertThat(r.getTipoRecursoId()).isNotNull();
    }

    @Test
    @DisplayName("Nace PENDIENTE y el saldo es el total menos el anticipo requerido")
    void desgloseDeDinero() {
        Reserva r = nueva(D1_15, D3_11, RECURSO, "500000", "150000");

        assertThat(r.getEstado()).isEqualTo(EstadoReserva.PENDIENTE);
        assertThat(r.getNoches()).isEqualTo(2);
        assertThat(r.numPersonas()).isEqualTo(2);
        assertThat(r.getTotal()).isEqualByComparingTo("500000");
        assertThat(r.getAnticipoRequerido()).isEqualByComparingTo("150000");
        assertThat(r.getSaldo()).isEqualByComparingTo("350000");
    }

    @Test
    @DisplayName("El anticipo nunca pasa del total; un total negativo queda en cero")
    void anticipoAcotado() {
        assertThat(nueva(D1_15, D3_11, RECURSO, "100000", "250000").getAnticipoRequerido())
                .isEqualByComparingTo("100000");
        Reserva negativo = nueva(D1_15, D3_11, RECURSO, "-5", "0");
        assertThat(negativo.getTotal()).isEqualByComparingTo("0");
        assertThat(negativo.getSaldo()).isEqualByComparingTo("0");
    }
}
