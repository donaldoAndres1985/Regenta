package com.regenta.menu.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** HU-080. Reglas puras de la disponibilidad de un ítem en un día de servicio. */
class DisponibilidadDiariaTest {

    private static DisponibilidadDiaria delDia() {
        return DisponibilidadDiaria.delDia(UUID.randomUUID(), UUID.randomUUID(),
                LocalDate.of(2026, 9, 9));
    }

    @Test
    @DisplayName("Nace disponible, sin cupo y sin ventas")
    void nace() {
        DisponibilidadDiaria d = delDia();
        assertThat(d.estaAgotado()).isFalse();
        assertThat(d.getCantidadDisponible()).isNull();
        assertThat(d.getCantidadVendida()).isZero();
    }

    @Test
    @DisplayName("Criterio 1: marcar agotado y reponer alternan el estado")
    void marcarYReponer() {
        DisponibilidadDiaria d = delDia();
        d.marcarAgotado();
        assertThat(d.estaAgotado()).isTrue();
        d.reponer();
        assertThat(d.estaAgotado()).isFalse();
    }

    @Test
    @DisplayName("Criterio 4: al llegar al cupo diario el ítem se marca agotado solo")
    void cupoSeAgotaSolo() {
        DisponibilidadDiaria d = delDia();
        d.fijarCupo(3);

        assertThat(d.registrarVenta(2)).as("todavía no llega al cupo").isFalse();
        assertThat(d.estaAgotado()).isFalse();

        assertThat(d.registrarVenta(1)).as("esta venta es la que lo agota").isTrue();
        assertThat(d.estaAgotado()).isTrue();

        assertThat(d.registrarVenta(1)).as("ya estaba agotado: no vuelve a notificar").isFalse();
    }

    @Test
    @DisplayName("Sin cupo, las ventas se acumulan pero no agotan")
    void sinCupoNoAgota() {
        DisponibilidadDiaria d = delDia();
        assertThat(d.registrarVenta(100)).isFalse();
        assertThat(d.estaAgotado()).isFalse();
        assertThat(d.getCantidadVendida()).isEqualTo(100);
    }

    @Test
    @DisplayName("Fijar un cupo por debajo de lo ya vendido agota de inmediato")
    void cupoRetroactivo() {
        DisponibilidadDiaria d = delDia();
        d.registrarVenta(5);
        assertThat(d.fijarCupo(4)).isTrue();
        assertThat(d.estaAgotado()).isTrue();
    }

    @Test
    @DisplayName("Una venta de cero o negativa no mueve la cuenta")
    void ventaNoPositiva() {
        DisponibilidadDiaria d = delDia();
        assertThat(d.registrarVenta(0)).isFalse();
        assertThat(d.registrarVenta(-3)).isFalse();
        assertThat(d.getCantidadVendida()).isZero();
    }
}
