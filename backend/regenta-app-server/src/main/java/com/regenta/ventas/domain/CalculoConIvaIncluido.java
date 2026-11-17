package com.regenta.ventas.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * HU-148 · los valores de una línea cuyo precio unitario es el de la etiqueta, con IVA incluido
 * (I2, I5 de {@code docs/dominio/inventario.md}).
 *
 * <ul>
 * <li>total = precio × cantidad − descuento (el descuento va sobre el precio con IVA);</li>
 * <li>base = total / (1 + t), a 2 decimales; impuesto = total − base;</li>
 * <li>subtotal = bruto / (1 + t) y descuento = subtotal − base, todo sin IVA, así
 * subtotal − descuento + impuesto = total.</li>
 * </ul>
 */
public record CalculoConIvaIncluido(BigDecimal subtotal, BigDecimal descuento, BigDecimal base,
        BigDecimal impuesto, BigDecimal total) {

    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final int ESCALA = 4;

    public static CalculoConIvaIncluido de(BigDecimal precioConIva, BigDecimal cantidad,
            BigDecimal descuentoPct, BigDecimal impuestoPct) {
        BigDecimal pct = impuestoPct == null ? BigDecimal.ZERO : impuestoPct;
        BigDecimal desc = descuentoPct == null ? BigDecimal.ZERO : descuentoPct;
        BigDecimal bruto = precioConIva.multiply(cantidad);
        BigDecimal total = bruto.subtract(bruto.multiply(desc).divide(CIEN, ESCALA + 2,
                RoundingMode.HALF_UP)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal base = sinIva(total, pct);
        BigDecimal subtotal = sinIva(bruto.setScale(2, RoundingMode.HALF_UP), pct);
        return new CalculoConIvaIncluido(escala(subtotal), escala(subtotal.subtract(base)),
                escala(base), escala(total.subtract(base)), escala(total));
    }

    private static BigDecimal sinIva(BigDecimal conIva, BigDecimal pct) {
        if (pct.signum() == 0) {
            return conIva;
        }
        return conIva.divide(BigDecimal.ONE.add(pct.divide(CIEN)), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal escala(BigDecimal v) {
        return v.setScale(ESCALA, RoundingMode.HALF_UP);
    }
}
