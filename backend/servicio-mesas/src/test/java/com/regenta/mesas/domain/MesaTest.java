package com.regenta.mesas.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.comun.errores.ReglaDeNegocioException;

/** HU-081. Reglas puras de una mesa: capacidad, código y posición en el plano. */
class MesaTest {

    private static Mesa mesa() {
        return Mesa.crear(UUID.randomUUID(), null, UUID.randomUUID(), "m1", "Ventana", 4,
                FormaDeMesa.REDONDA, 120, 60, 90, 90);
    }

    @Test
    @DisplayName("Criterio 1: una mesa nace con código, capacidad, zona, forma y posición")
    void nace() {
        Mesa m = mesa();
        assertThat(m.getCodigo()).isEqualTo("M1");
        assertThat(m.getCapacidad()).isEqualTo((short) 4);
        assertThat(m.getForma()).isEqualTo(FormaDeMesa.REDONDA);
        assertThat(m.getEstado()).isEqualTo(EstadoDeMesa.LIBRE);
        assertThat(m.getPosX()).isEqualTo(120);
        assertThat(m.getPosY()).isEqualTo(60);
        assertThat(m.getAncho()).isEqualTo(90);
        assertThat(m.isActiva()).isTrue();
    }

    @Test
    @DisplayName("El código se normaliza a mayúsculas y sin espacios")
    void codigoNormalizado() {
        Mesa m = Mesa.crear(UUID.randomUUID(), null, null, "  t04 ", null, null, null, null, null,
                null, null);
        assertThat(m.getCodigo()).isEqualTo("T04");
    }

    @Test
    @DisplayName("Una mesa sin código o con capacidad no positiva se rechaza")
    void validaciones() {
        assertThatThrownBy(() -> Mesa.crear(UUID.randomUUID(), null, null, "  ", null, null, null,
                null, null, null, null)).isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> Mesa.crear(UUID.randomUUID(), null, null, "M2", null, 0, null,
                null, null, null, null)).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Criterio 3: mover la mesa guarda la nueva posición")
    void mover() {
        Mesa m = mesa();
        m.mover(300, 220, null, null);
        assertThat(m.getPosX()).isEqualTo(300);
        assertThat(m.getPosY()).isEqualTo(220);
        assertThat(m.getAncho()).isEqualTo(90); // no se tocó
    }

    @Test
    @DisplayName("Una posición negativa se recorta a cero; un tamaño no positivo, a uno")
    void moverConLimites() {
        Mesa m = mesa();
        m.mover(-50, -1, 0, -10);
        assertThat(m.getPosX()).isZero();
        assertThat(m.getPosY()).isZero();
        assertThat(m.getAncho()).isEqualTo(1);
        assertThat(m.getAlto()).isEqualTo(1);
    }

    @Test
    @DisplayName("Editar cambia zona, nombre, capacidad y forma pero no el código")
    void editar() {
        Mesa m = mesa();
        UUID otraZona = UUID.randomUUID();
        m.editar(otraZona, "Rincón", 6, FormaDeMesa.RECTANGULAR);
        assertThat(m.getZonaId()).isEqualTo(otraZona);
        assertThat(m.getNombre()).isEqualTo("Rincón");
        assertThat(m.getCapacidad()).isEqualTo((short) 6);
        assertThat(m.getForma()).isEqualTo(FormaDeMesa.RECTANGULAR);
        assertThat(m.getCodigo()).isEqualTo("M1");
    }
}
