package com.regenta.menu.aplicacion;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Una línea de comanda a explotar contra su receta (HU-079): un ítem, una
 * cantidad y los modificadores elegidos que descuentan insumo.
 */
public record LineaAExplotar(
        @NotNull UUID itemMenuId,
        @NotNull @Positive BigDecimal cantidad,
        List<UUID> modificadorIds) {
}
