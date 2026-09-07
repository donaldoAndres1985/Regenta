package com.regenta.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Un producto y una cantidad a apartar en una bodega. */
public record LineaDeReserva(
        @NotNull UUID productoId,
        @NotNull UUID bodegaId,
        @NotNull @Positive BigDecimal cantidad) {
}
