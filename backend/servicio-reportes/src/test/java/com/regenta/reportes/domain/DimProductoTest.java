package com.regenta.reportes.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** HU-096. Reglas puras de la dimensión producto: cuándo hace falta una versión nueva. */
class DimProductoTest {

    private static DimProducto version() {
        return DimProducto.nuevaVersion(UUID.randomUUID(), UUID.randomUUID(), "SKU-1", "Bandeja paisa",
                UUID.randomUUID(), "Fuertes", new BigDecimal("32000"), new BigDecimal("12000"));
    }

    @Test
    @DisplayName("Nace vigente, sin fecha de cierre")
    void nace() {
        DimProducto d = version();
        assertThat(d.isEsActual()).isTrue();
        assertThat(d.getVigenteHasta()).isNull();
    }

    @Test
    @DisplayName("Cerrar deja de ser la versión actual")
    void cerrar() {
        DimProducto d = version();
        d.cerrar();
        assertThat(d.isEsActual()).isFalse();
        assertThat(d.getVigenteHasta()).isNotNull();
    }

    @Test
    @DisplayName("Criterio 2: cambiar el nombre exige una versión nueva")
    void cambioDeNombre() {
        DimProducto d = version();
        assertThat(d.cambioFrenteA("Bandeja paisa", "Fuertes", new BigDecimal("32000"),
                new BigDecimal("12000"))).isFalse();
        assertThat(d.cambioFrenteA("Bandeja paisa grande", "Fuertes", new BigDecimal("32000"),
                new BigDecimal("12000"))).isTrue();
    }

    @Test
    @DisplayName("Cambiar precio o costo también exige una versión nueva")
    void cambioDePrecioOCosto() {
        DimProducto d = version();
        assertThat(d.cambioFrenteA("Bandeja paisa", "Fuertes", new BigDecimal("35000"),
                new BigDecimal("12000"))).isTrue();
        assertThat(d.cambioFrenteA("Bandeja paisa", "Fuertes", new BigDecimal("32000"),
                new BigDecimal("13000"))).isTrue();
    }

    @Test
    @DisplayName("32000 y 32000.00 no son un cambio: se comparan por valor, no por escala")
    void comparaPorValorNumerico() {
        DimProducto d = version();
        assertThat(d.cambioFrenteA("Bandeja paisa", "Fuertes", new BigDecimal("32000.00"),
                new BigDecimal("12000.0000"))).isFalse();
    }
}
