package com.regenta.inventario.domain;

import java.math.BigDecimal;

/**
 * Cómo grava el IVA a un producto (I1 de {@code docs/dominio/inventario.md}).
 *
 * <p>{@link #EXENTO} y {@link #EXCLUIDO} dan IVA 0, pero no son lo mismo para la DIAN: el
 * exento está gravado a tarifa 0 (en la factura sale IVA 0 %); el excluido no causa IVA (en la
 * factura no lleva el tributo).
 */
public enum TarifaIva {

    IVA_19(new BigDecimal("19"), true),
    IVA_5(new BigDecimal("5"), true),
    EXENTO(BigDecimal.ZERO, true),
    EXCLUIDO(BigDecimal.ZERO, false);

    private final BigDecimal porcentaje;
    private final boolean causaIva;

    TarifaIva(BigDecimal porcentaje, boolean causaIva) {
        this.porcentaje = porcentaje;
        this.causaIva = causaIva;
    }

    public BigDecimal porcentaje() {
        return porcentaje;
    }

    /** Si el producto causa IVA (aunque sea a 0 %). El excluido no lo causa. */
    public boolean causaIva() {
        return causaIva;
    }
}
