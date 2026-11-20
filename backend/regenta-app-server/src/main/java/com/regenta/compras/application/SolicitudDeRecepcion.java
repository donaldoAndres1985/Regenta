package com.regenta.compras.application;

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
 * Lo que se recibe de una orden de compra (HU-048). El lote y el vencimiento se capturan aquí,
 * línea por línea. Si el producto los exige lo decide el backend con el catálogo de Inventario
 * (HU-165, I16 de Compras), no el cliente.
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
            /**
             * Ya no se usa: el backend consulta el catálogo (HU-165). Se acepta para no romper a
             * los clientes que todavía lo mandan.
             */
            @Deprecated boolean exigeLote,
            String codigoLote,
            LocalDate fechaVencimiento,
            String registroSanitario) {
    }
}
