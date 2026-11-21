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
        /** HU-168 · Descuento a pie de factura; se reparte entre las líneas por su valor. */
        @PositiveOrZero BigDecimal descuentoGlobal,
        @Size(max = 2000) String observaciones,
        @NotEmpty @Valid List<LineaDeCompra> lineas,
        /** HU-169 · {@code CONTADO} o {@code CREDITO}; si no viene, a crédito. */
        String condicionPago,
        /** HU-169 · Con qué se paga una compra de contado: EFECTIVO, TRANSFERENCIA, CHEQUE, TARJETA, OTRO. */
        String metodoPago,
        /** HU-169 / HU-145 · De contado en efectivo con la plata de la caja abierta del usuario. */
        Boolean pagarDesdeLaCaja,
        @Size(max = 60) String referenciaPago) {

    /** La compra a crédito de HU-167, sin datos de pago. */
    public SolicitudDeCompraDirecta(UUID proveedorId, UUID bodegaId, String facturaProveedor,
            LocalDate fechaFactura, BigDecimal flete, BigDecimal descuentoGlobal, String observaciones,
            List<LineaDeCompra> lineas) {
        this(proveedorId, bodegaId, facturaProveedor, fechaFactura, flete, descuentoGlobal,
                observaciones, lineas, null, null, null, null);
    }

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
