package com.regenta.inventario.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * El precio de la etiqueta, con IVA incluido, y lo que lleva adentro (I2 de
 * {@code docs/dominio/inventario.md}): base = precio / (1 + tarifa) a 2 decimales e IVA = precio
 * − base, así base + IVA siempre da exactamente el precio. Cambiar la tarifa no cambia el precio:
 * cambia la base.
 */
public record PrecioConIva(BigDecimal precio, TarifaIva tarifa) {

    private static final BigDecimal CIEN = new BigDecimal("100");

    public BigDecimal base() {
        if (tarifa.porcentaje().signum() == 0) {
            return precio;
        }
        BigDecimal divisor = BigDecimal.ONE.add(tarifa.porcentaje().divide(CIEN));
        return precio.divide(divisor, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal iva() {
        return precio.subtract(base());
    }
}
