package com.regenta.compras.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Asocia un producto a un proveedor, con su código y su costo (HU-046 criterio 2). */
public record SolicitudDeProductoDeProveedor(
        @NotNull UUID productoId,
        @Size(max = 60) String codigoProveedor,
        @PositiveOrZero BigDecimal costoUltimo,
        @PositiveOrZero Integer diasEntrega,
        @PositiveOrZero BigDecimal cantidadMinima,
        boolean preferido) {
}
