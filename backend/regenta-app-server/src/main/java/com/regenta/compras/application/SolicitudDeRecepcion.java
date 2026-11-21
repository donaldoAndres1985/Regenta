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
import jakarta.validation.constraints.Size;

/**
 * Lo que se recibe de una orden de compra (HU-048, HU-175). El lote y el vencimiento se capturan
 * aquí, línea por línea. Si el producto los exige lo decide el backend con el catálogo de Inventario
 * (HU-165, I16 de Compras), no el cliente.
 *
 * <p>HU-175 C4 · La factura llega igual que en la compra directa: número, fecha, flete, descuento
 * global y si se paga de contado o queda a crédito.
 */
public record SolicitudDeRecepcion(
        @NotNull UUID ordenId,
        UUID bodegaId,
        @Size(max = 40) String facturaProveedor,
        @Size(max = 2000) String observaciones,
        @NotEmpty @Valid List<LineaDeRecepcion> lineas,
        LocalDate fechaFactura,
        @PositiveOrZero BigDecimal flete,
        @PositiveOrZero BigDecimal descuentoGlobal,
        /** {@code CONTADO} o {@code CREDITO}; si no viene, a crédito. */
        String condicionPago,
        String metodoPago,
        Boolean pagarDesdeLaCaja,
        @Size(max = 60) String referenciaPago) {

    /** La recepción de HU-048: a crédito, sin flete ni descuento global. */
    public SolicitudDeRecepcion(UUID ordenId, UUID bodegaId, String facturaProveedor,
            String observaciones, List<LineaDeRecepcion> lineas) {
        this(ordenId, bodegaId, facturaProveedor, observaciones, lineas, null, null, null, null, null,
                null, null);
    }

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
