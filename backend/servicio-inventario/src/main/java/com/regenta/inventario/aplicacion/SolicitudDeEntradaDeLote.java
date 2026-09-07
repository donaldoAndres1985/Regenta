package com.regenta.inventario.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Entrada de mercancia de un producto que maneja lotes. El {@code codigoLote}
 * es obligatorio: es el criterio 1 de HU-031.
 */
public record SolicitudDeEntradaDeLote(
        @NotNull UUID productoId,
        @NotNull UUID bodegaId,
        @NotBlank @Size(max = 60) String codigoLote,
        @NotNull @Positive BigDecimal cantidad,
        LocalDate fechaFabricacion,
        LocalDate fechaVencimiento,
        @Size(max = 60) String registroSanitario,
        BigDecimal costoUnitario,
        String motivo,
        @NotBlank String idempotencyKey) {
}
