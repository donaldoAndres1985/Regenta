package com.regenta.caja.aplicacion;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Un ingreso de efectivo a la caja (HU-061 criterio 3). */
public record SolicitudDeIngreso(
        @NotNull @Positive BigDecimal monto,
        @NotBlank @Size(max = 200) String concepto) {
}
