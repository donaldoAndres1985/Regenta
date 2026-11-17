package com.regenta.inventario.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.shared.web.ReglaDeNegocioException;

/** HU-147 · la tarifa de IVA vive en el producto (I1, I2, I6). */
class TarifaDelProductoTest {

    private static Producto barniz(TarifaIva tarifa) {
        Producto p = Producto.nuevo(UUID.randomUUID(), "7707031403512", "Barniz galón",
                UUID.randomUUID(), UUID.randomUUID(), tarifa, UUID.randomUUID());
        p.datosBasicos(null, null, null, new BigDecimal("35500"), null);
        return p;
    }

    @Test
    @DisplayName("HU-147 C1 · Dado un producto nuevo sin tarifa, se rechaza con «Elige la tarifa de IVA del producto.»")
    void sinTarifaNoSeCrea() {
        assertThatThrownBy(() -> barniz(null))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("Elige la tarifa de IVA del producto.");
    }

    @Test
    @DisplayName("HU-147 C4 · Dado un producto de $ 35.500 con IVA_19, al cambiar a IVA_5 el precio sigue en $ 35.500 y la base pasa a $ 33.809,52")
    void cambiarLaTarifaNoCambiaElPrecio() {
        Producto p = barniz(TarifaIva.IVA_19);

        p.cambiarTarifa(TarifaIva.IVA_5);

        assertThat(p.getPrecioVenta()).isEqualByComparingTo("35500");
        assertThat(p.precio().base()).isEqualByComparingTo("33809.52");
    }

    @Test
    @DisplayName("I6 · Un producto creado con su tarifa no queda «por revisar»; confirmar la tarifa quita la marca")
    void confirmarLaTarifaQuitaLaMarca() {
        Producto p = barniz(TarifaIva.IVA_19);
        assertThat(p.isTarifaPorRevisar()).isFalse();

        p.cambiarTarifa(TarifaIva.IVA_19);

        assertThat(p.isTarifaPorRevisar()).isFalse();
        assertThat(p.getTarifaIva()).isEqualTo(TarifaIva.IVA_19);
    }

    @Test
    @DisplayName("I1 · Cambiar a una tarifa nula se rechaza")
    void noSeQuedaSinTarifa() {
        Producto p = barniz(TarifaIva.IVA_19);

        assertThatThrownBy(() -> p.cambiarTarifa(null)).isInstanceOf(ReglaDeNegocioException.class);
    }
}
