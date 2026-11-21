package com.regenta.compras.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Al confirmar la recepción se puede adjuntar (o corregir) la factura del proveedor y, HU-179, fijar el
 * precio de venta nuevo (con IVA incluido) de los productos que llegaron. Sin precio, no cambia.
 */
public record SolicitudDeConfirmacionDeRecepcion(
        String facturaProveedor,
        @Valid List<PrecioNuevo> preciosNuevos) {

    public SolicitudDeConfirmacionDeRecepcion(String facturaProveedor) {
        this(facturaProveedor, null);
    }

    public record PrecioNuevo(@NotNull UUID productoId, @NotNull @Positive BigDecimal precioVenta) {
    }
}
