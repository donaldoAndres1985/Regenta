package com.regenta.caja.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Abre una sesión de caja con su base (HU-059). */
public record SolicitudDeApertura(
        @NotNull UUID cajaId,
        @NotNull @PositiveOrZero BigDecimal montoApertura) {
}
