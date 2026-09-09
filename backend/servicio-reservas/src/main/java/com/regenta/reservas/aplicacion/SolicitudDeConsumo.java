package com.regenta.reservas.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Cargar un consumo a la estancia (HU-073). {@code productoId} lo enlaza a un
 * artículo de inventario; {@code comandaId}, a una comanda de restaurante.
 */
public record SolicitudDeConsumo(
        String origen,
        UUID productoId,
        UUID comandaId,
        @NotBlank @Size(max = 180) String descripcion,
        @NotNull BigDecimal cantidad,
        @NotNull @PositiveOrZero BigDecimal precioUnitario,
        @PositiveOrZero BigDecimal impuestoPct) {
}
