package com.regenta.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Fija (o ajusta) el precio de un producto en una lista a partir de cierta
 * cantidad. Repetir con otra {@code cantidadMinima} agrega un tramo por volumen.
 */
public record SolicitudDePrecio(
        @NotNull UUID listaId,
        @NotNull UUID productoId,
        @NotNull @PositiveOrZero BigDecimal precio,
        @PositiveOrZero BigDecimal descuentoMaxPct,
        @NotNull @Positive BigDecimal cantidadMinima) {
}
