package com.regenta.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Salida de un lote concreto. Si el lote esta vencido, {@code autorizacionVencido}
 * debe traer el motivo de la autorizacion; si no, la salida se rechaza
 * (criterio 4 de HU-031).
 */
public record SolicitudDeSalidaDeLote(
        @NotNull UUID productoId,
        @NotNull UUID bodegaId,
        @NotBlank @Size(max = 60) String codigoLote,
        @NotNull @Positive BigDecimal cantidad,
        String autorizacionVencido,
        String motivo,
        @NotBlank String idempotencyKey) {
}
