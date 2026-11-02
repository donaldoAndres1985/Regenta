package com.regenta.inventario.application;

import com.regenta.inventario.api.*;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Lo que llegÃ³ de una lÃ­nea concreta al recibir el traslado. */
public record LineaRecibida(
        @NotNull UUID lineaId,
        @NotNull @PositiveOrZero BigDecimal cantidadRecibida) {
}
