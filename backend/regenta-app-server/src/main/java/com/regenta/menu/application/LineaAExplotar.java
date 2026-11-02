package com.regenta.menu.application;

import com.regenta.menu.api.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Una lÃ­nea de comanda a explotar contra su receta (HU-079): un Ã­tem, una
 * cantidad y los modificadores elegidos que descuentan insumo.
 */
public record LineaAExplotar(
        @NotNull UUID itemMenuId,
        @NotNull @Positive BigDecimal cantidad,
        List<UUID> modificadorIds) {
}
