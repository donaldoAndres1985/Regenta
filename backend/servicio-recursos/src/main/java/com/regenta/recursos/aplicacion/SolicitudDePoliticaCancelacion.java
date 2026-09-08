package com.regenta.recursos.aplicacion;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Alta o edición de una política de cancelación (HU-068). Los pct son fracción: 0.25 = 25 %. */
public record SolicitudDePoliticaCancelacion(
        @NotBlank @Size(max = 80) String nombre,
        @NotNull @PositiveOrZero Integer horasAntes,
        @NotNull @PositiveOrZero @DecimalMax("1.0") BigDecimal penalizacionPct,
        @NotNull @PositiveOrZero @DecimalMax("1.0") BigDecimal anticipoRequeridoPct,
        boolean esDefault) {
}
