package com.regenta.recursos.application;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Alta o ediciÃ³n de una polÃ­tica de cancelaciÃ³n (HU-068). Los pct son fracciÃ³n: 0.25 = 25 %. */
public record SolicitudDePoliticaCancelacion(
        @NotBlank @Size(max = 80) String nombre,
        @NotNull @PositiveOrZero Integer horasAntes,
        @NotNull @PositiveOrZero @DecimalMax("1.0") BigDecimal penalizacionPct,
        @NotNull @PositiveOrZero @DecimalMax("1.0") BigDecimal anticipoRequeridoPct,
        boolean esDefault) {
}
