package com.regenta.compras.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * HU-167 · Una compra directa: la mercancía que llegó con la factura del proveedor, sin orden.
 *
 * <p>La tarifa de IVA y la exigencia de lote no se mandan: las decide el catálogo de Inventario.
 * Si una línea no trae costo, se usa el último costo del catálogo del proveedor.
 */
public record SolicitudDeCompraDirecta(
        @NotNull UUID proveedorId,
        @NotNull UUID bodegaId,
        @Size(max = 40) String facturaProveedor,
        LocalDate fechaFactura,
        @PositiveOrZero BigDecimal flete,
        @Size(max = 2000) String observaciones,
        @NotEmpty @Valid List<LineaDeCompra> lineas) {

    public record LineaDeCompra(
            @NotNull UUID productoId,
            @NotNull @Positive BigDecimal cantidad,
            @PositiveOrZero BigDecimal costoUnitario,
            @PositiveOrZero @DecimalMax(value = "100", inclusive = false) BigDecimal descuentoPct,
            @Size(max = 60) String codigoProveedor,
            @Size(max = 60) String codigoLote,
            LocalDate fechaVencimiento,
            @Size(max = 60) String registroSanitario) {
    }
}
