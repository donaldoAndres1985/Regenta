package com.regenta.menu.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Alta o edición de una línea de receta (HU-079). {@code mermaPct} es fracción: 0.10 = 10 %. */
public record SolicitudDeLineaDeReceta(
        @NotNull UUID productoId,
        @Size(max = 180) String nombreSnapshot,
        @NotNull @Positive BigDecimal cantidad,
        @Size(max = 20) String unidad,
        @PositiveOrZero BigDecimal mermaPct,
        boolean opcional) {
}
