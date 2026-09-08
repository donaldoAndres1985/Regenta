package com.regenta.caja.aplicacion;

import java.math.BigDecimal;

import com.regenta.caja.domain.MetodoPagoCaja;

/** El neto y la propina de una sesión por método de pago (HU-063). */
public record TotalPorMetodo(String metodo, BigDecimal total, BigDecimal propina, long movimientos) {

    public TotalPorMetodo(MetodoPagoCaja metodo, BigDecimal total, BigDecimal propina,
            long movimientos) {
        this(metodo.name(), total == null ? BigDecimal.ZERO : total,
                propina == null ? BigDecimal.ZERO : propina, movimientos);
    }
}
