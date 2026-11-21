package com.regenta.compras.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * HU-179 C1 / I20 · Cómo cambia el margen de un producto con lo que cuesta ahora: costo anterior (el
 * promedio en la bodega) y nuevo (el de entrada de esta recepción), el precio de venta con IVA y su
 * base, y el margen sobre la base sin IVA antes y después, en porcentaje con un decimal.
 */
public record MargenDeLinea(
        UUID recepcionLineaId,
        UUID productoId,
        String nombre,
        BigDecimal costoAnterior,
        BigDecimal costoNuevo,
        BigDecimal precioVenta,
        BigDecimal base,
        BigDecimal margenAnterior,
        BigDecimal margenNuevo,
        boolean cambiaElCosto,
        /** El precio que se fijó al confirmar, si se fijó. */
        BigDecimal precioVentaNuevo) {

    private static final BigDecimal CIEN = new BigDecimal("100");

    /** El margen sobre la base: (base − costo) ÷ base; nulo si no hay precio. */
    public static BigDecimal margen(BigDecimal base, BigDecimal costo) {
        if (base == null || base.signum() <= 0 || costo == null) {
            return null;
        }
        return base.subtract(costo).multiply(CIEN).divide(base, 1, RoundingMode.HALF_UP);
    }
}
