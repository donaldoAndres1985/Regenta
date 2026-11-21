package com.regenta.inventario.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** HU-177 · El costo promedio después de sacar lo que entró con una recepción anulada. */
class SalidaPorAnulacionDeCompraTest {

    private static BigDecimal promedio(String cantidadAntes, String promedioAntes, String cantidad, String costo) {
        return SalidaPorAnulacionDeCompra.promedioSinLaCompra(new BigDecimal(cantidadAntes),
                new BigDecimal(promedioAntes), new BigDecimal(cantidad), new BigDecimal(costo));
    }

    @Test
    @DisplayName("20 a $ 9.000 más 10 a $ 10.500 dan 30 a $ 9.500; anular las 10 vuelve a $ 9.000")
    void vuelveAlPromedioAnterior() {
        assertThat(promedio("30", "9500", "10", "10500")).isEqualByComparingTo("9000");
    }

    @Test
    @DisplayName("Si no queda nada, se conserva el promedio que había")
    void sinNadaQueda() {
        assertThat(promedio("10", "10500", "10", "10500")).isEqualByComparingTo("10500");
    }

    @Test
    @DisplayName("Si las salidas de por medio dejarían un valor negativo, se conserva el promedio")
    void valorNegativo() {
        assertThat(promedio("12", "1000", "10", "5000")).isEqualByComparingTo("1000");
    }
}
