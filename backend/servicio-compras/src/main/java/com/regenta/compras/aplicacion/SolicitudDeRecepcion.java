package com.regenta.compras.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Lo que se recibe de una orden de compra (HU-048). El lote y el vencimiento se
 * capturan acá, línea por línea, y solo se guardan si la categoría del producto
 * los exige (`exigeLote`, que el cliente trae de la config de inventario, HU-031).
 */
public record SolicitudDeRecepcion(
        @NotNull UUID ordenId,
        UUID bodegaId,
        String facturaProveedor,
        String observaciones,
        @NotEmpty @Valid List<LineaDeRecepcion> lineas) {

    public record LineaDeRecepcion(
            @NotNull UUID ordenLineaId,
            @NotNull @Positive BigDecimal cantidad,
            @PositiveOrZero BigDecimal costoUnitario,
            boolean exigeLote,
            String codigoLote,
            LocalDate fechaVencimiento,
            String registroSanitario) {
    }
}
