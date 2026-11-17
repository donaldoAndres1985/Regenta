package com.regenta.inventario.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** HU-147 · el precio guardado es el de la etiqueta, con IVA incluido (I2 de docs/dominio/inventario.md). */
class PrecioConIvaTest {

    private static final BigDecimal ETIQUETA = new BigDecimal("35500");

    @Test
    @DisplayName("HU-147 C2 · Dado un producto de $ 35.500 con IVA_19, la base es $ 29.831,93 y el IVA $ 5.668,07")
    void conIva19SeSeparaBaseEIva() {
        PrecioConIva precio = new PrecioConIva(ETIQUETA, TarifaIva.IVA_19);

        assertThat(precio.base()).isEqualByComparingTo("29831.93");
        assertThat(precio.iva()).isEqualByComparingTo("5668.07");
        assertThat(precio.base().add(precio.iva())).isEqualByComparingTo(ETIQUETA);
    }

    @Test
    @DisplayName("HU-147 C3 · Dado un producto EXENTO o EXCLUIDO, la base es el precio y el IVA $ 0")
    void exentoYExcluidoNoLlevanIva() {
        for (TarifaIva tarifa : new TarifaIva[] { TarifaIva.EXENTO, TarifaIva.EXCLUIDO }) {
            PrecioConIva precio = new PrecioConIva(ETIQUETA, tarifa);

            assertThat(precio.base()).isEqualByComparingTo(ETIQUETA);
            assertThat(precio.iva()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Test
    @DisplayName("HU-147 C4 · Con IVA_5, el mismo precio de $ 35.500 da base $ 33.809,52")
    void conIva5CambiaLaBaseNoElPrecio() {
        PrecioConIva precio = new PrecioConIva(ETIQUETA, TarifaIva.IVA_5);

        assertThat(precio.precio()).isEqualByComparingTo(ETIQUETA);
        assertThat(precio.base()).isEqualByComparingTo("33809.52");
        assertThat(precio.iva()).isEqualByComparingTo("1690.48");
    }

    @Test
    @DisplayName("Exento y excluido son tarifas distintas aunque ambas den IVA 0 (la DIAN los trata distinto)")
    void exentoNoEsExcluido() {
        assertThat(TarifaIva.EXENTO.porcentaje()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(TarifaIva.EXCLUIDO.porcentaje()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(TarifaIva.EXENTO.causaIva()).isTrue();
        assertThat(TarifaIva.EXCLUIDO.causaIva()).isFalse();
    }
}
