package com.regenta.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

import com.regenta.inventario.domain.OrigenMovimiento;
import com.regenta.inventario.domain.TipoMovimiento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Un movimiento a anotar en el libro. La {@code idempotencyKey} es lo que evita
 * el doble descuento cuando un evento se entrega dos veces.
 */
public record SolicitudDeMovimiento(
        @NotNull UUID productoId,
        @NotNull UUID bodegaId,
        @NotNull TipoMovimiento tipo,
        @NotNull @Positive BigDecimal cantidad,
        @NotNull OrigenMovimiento origenTipo,
        UUID origenId,
        String motivo,
        @NotBlank String idempotencyKey) {
}
