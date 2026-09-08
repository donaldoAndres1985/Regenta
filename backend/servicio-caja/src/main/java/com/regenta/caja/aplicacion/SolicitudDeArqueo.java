package com.regenta.caja.aplicacion;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/** El conteo de efectivo por denominación al preparar el cierre (HU-062). */
public record SolicitudDeArqueo(
        @NotEmpty @Valid List<LineaDeArqueo> denominaciones) {

    public record LineaDeArqueo(
            @NotNull @Positive BigDecimal denominacion,
            String tipo,
            @PositiveOrZero int cantidad) {
    }
}
