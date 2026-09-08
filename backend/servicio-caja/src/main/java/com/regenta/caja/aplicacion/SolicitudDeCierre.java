package com.regenta.caja.aplicacion;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Cierra la sesión declarando lo contado (HU-059). */
public record SolicitudDeCierre(
        @NotNull @PositiveOrZero BigDecimal montoDeclarado,
        String observaciones) {
}
