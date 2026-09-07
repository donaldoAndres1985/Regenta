package com.regenta.ventas.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Una línea a agregar. El SKU, el nombre, el precio, el impuesto y el costo
 * llegan resueltos: son el snapshot que la venta guarda (HU-037, criterio 1).
 */
public record SolicitudDeLinea(
        @NotNull UUID productoId,
        @NotBlank @Size(max = 60) String sku,
        @NotBlank @Size(max = 180) String nombre,
        @Size(max = 20) String unidad,
        @NotNull @Positive BigDecimal cantidad,
        @NotNull @PositiveOrZero BigDecimal precioUnitario,
        @PositiveOrZero BigDecimal descuentoPct,
        @Size(max = 20) String impuestoCodigo,
        @PositiveOrZero BigDecimal impuestoPct,
        @PositiveOrZero BigDecimal costoUnitario) {
}
