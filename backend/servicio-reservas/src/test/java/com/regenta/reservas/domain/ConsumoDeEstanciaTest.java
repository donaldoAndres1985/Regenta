package com.regenta.reservas.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.comun.errores.ReglaDeNegocioException;

/** HU-073. El total de un cargo a la habitación sale de cantidad × precio × (1 + impuesto). */
class ConsumoDeEstanciaTest {

    private static final UUID NEG = UUID.randomUUID();
    private static final UUID EST = UUID.randomUUID();

    private static ConsumoDeEstancia consumo(String cantidad, String precio, String impuesto,
            UUID productoId) {
        return ConsumoDeEstancia.nuevo(NEG, EST, OrigenConsumo.MINIBAR, productoId, null,
                "Gaseosa", new BigDecimal(cantidad), new BigDecimal(precio),
                new BigDecimal(impuesto), null);
    }

    @Test
    @DisplayName("El total incluye impuesto: 2 × 12000 × 1.19 = 28560")
    void totalConImpuesto() {
        assertThat(consumo("2", "12000", "0.19", null).getTotal()).isEqualByComparingTo("28560");
        assertThat(consumo("1", "50000", "0", null).getTotal()).isEqualByComparingTo("50000");
    }

    @Test
    @DisplayName("Un consumo con producto descuenta inventario; sin producto, no")
    void descuentaInventario() {
        assertThat(consumo("1", "10", "0", UUID.randomUUID()).descuentaInventario()).isTrue();
        assertThat(consumo("1", "10", "0", null).descuentaInventario()).isFalse();
    }

    @Test
    @DisplayName("Descripción vacía, cantidad no positiva o precio negativo se rechazan")
    void validaciones() {
        assertThatThrownBy(() -> ConsumoDeEstancia.nuevo(NEG, EST, OrigenConsumo.OTRO, null, null,
                "  ", BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO, null))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> consumo("0", "10", "0", null))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> consumo("1", "-1", "0", null))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Sin origen, el consumo queda como OTRO")
    void origenPorDefecto() {
        assertThat(OrigenConsumo.desde(null)).isEqualTo(OrigenConsumo.OTRO);
        assertThatThrownBy(() -> OrigenConsumo.desde("PISCINA"))
                .isInstanceOf(ReglaDeNegocioException.class);
    }
}
