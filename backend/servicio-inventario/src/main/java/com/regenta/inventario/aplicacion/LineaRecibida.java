package com.regenta.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Lo que llegó de una línea concreta al recibir el traslado. */
public record LineaRecibida(
        @NotNull UUID lineaId,
        @NotNull @PositiveOrZero BigDecimal cantidadRecibida) {
}
