package com.regenta.ventas.aplicacion;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Una línea de la venta y cuánto de ella se devuelve. */
public record LineaDevuelta(
        @NotNull Short linea,
        @NotNull @Positive BigDecimal cantidad) {
}
