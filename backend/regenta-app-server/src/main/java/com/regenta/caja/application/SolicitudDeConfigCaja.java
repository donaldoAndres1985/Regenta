package com.regenta.caja.application;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Fija el umbral de retiro que exige autorización (HU-061). 0 = sin límite. */
public record SolicitudDeConfigCaja(
        @NotNull @PositiveOrZero BigDecimal retiroMaxSinAutorizacion) {
}
