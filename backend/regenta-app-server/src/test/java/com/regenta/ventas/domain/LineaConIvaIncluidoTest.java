package com.regenta.ventas.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * HU-148 · la línea de venta parte del precio de la etiqueta, con IVA incluido (I2, I3, I5 de
 * docs/dominio/inventario.md): total = precio × cantidad − descuento; base = total / (1 + t);
 * IVA = total − base.
 */
class LineaConIvaIncluidoTest {

    private static VentaLinea linea(String precio, String descuentoPct, String codigo, String pct) {
        return VentaLinea.de(UUID.randomUUID(), UUID.randomUUID(), (short) 1, UUID.randomUUID(),
                "7707031403512", "Barniz galón", "GLN", BigDecimal.ONE, new BigDecimal(precio),
                new BigDecimal(descuentoPct), codigo, new BigDecimal(pct), BigDecimal.ZERO);
    }

    @Test
    @DisplayName("HU-148 C1 · Un producto IVA_19 de $ 35.500 da base $ 29.831,93, IVA $ 5.668,07 y total $ 35.500")
    void conIva19ElTotalEsElDeLaEtiqueta() {
        VentaLinea l = linea("35500", "0", "IVA_19", "19");

        assertThat(l.getTotal()).isEqualByComparingTo("35500");
        assertThat(l.getBaseGravable()).isEqualByComparingTo("29831.93");
        assertThat(l.getImpuestoValor()).isEqualByComparingTo("5668.07");
    }

    @Test
    @DisplayName("HU-148 C2 · Sin IVA (negocio no responsable), la base es el precio completo")
    void sinIvaLaBaseEsElPrecio() {
        VentaLinea l = linea("35500", "0", "NO_RESPONSABLE", "0");

        assertThat(l.getTotal()).isEqualByComparingTo("35500");
        assertThat(l.getBaseGravable()).isEqualByComparingTo("35500");
        assertThat(l.getImpuestoValor()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("HU-148 C3 · Con 10 % de descuento el total es $ 31.950 y base e IVA se recalculan en proporción")
    void elDescuentoVaSobreElPrecioConIva() {
        VentaLinea l = linea("35500", "10", "IVA_19", "19");

        assertThat(l.getTotal()).isEqualByComparingTo("31950");
        assertThat(l.getBaseGravable()).isEqualByComparingTo("26848.74");
        assertThat(l.getImpuestoValor()).isEqualByComparingTo("5101.26");
        assertThat(l.getSubtotal().subtract(l.getDescuentoValor()))
                .isEqualByComparingTo(l.getBaseGravable());
    }

    @Test
    @DisplayName("HU-148 · Cambiar la cantidad recalcula con el precio y la tarifa ya copiados")
    void cambiarLaCantidadUsaLoCopiado() {
        VentaLinea l = linea("35500", "0", "IVA_19", "19");

        l.cambiarCantidad(new BigDecimal("2"));

        assertThat(l.getTotal()).isEqualByComparingTo("71000");
        assertThat(l.getBaseGravable().add(l.getImpuestoValor())).isEqualByComparingTo("71000");
    }

    @Test
    @DisplayName("HU-148 C5 · La línea de cotización sigue la misma regla")
    void laCotizacionTambienIncluyeElIva() {
        CotizacionLinea l = CotizacionLinea.de(UUID.randomUUID(), UUID.randomUUID(), (short) 1,
                UUID.randomUUID(), "7707031403512", "Barniz galón", "GLN", BigDecimal.ONE,
                new BigDecimal("35500"), new BigDecimal("10"), "IVA_19", new BigDecimal("19"),
                BigDecimal.ZERO);

        assertThat(l.getTotal()).isEqualByComparingTo("31950");
    }
}
