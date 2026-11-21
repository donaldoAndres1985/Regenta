package com.regenta.compras.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Lo que se pide para armar una orden de compra (HU-047). El costo de cada lÃ­nea
 * es opcional: si no viene, se toma del que el proveedor tiene registrado para
 * ese producto (HU-046 criterio 2).
 */
public record SolicitudDeOrden(
        @NotNull UUID proveedorId,
        @NotNull UUID bodegaDestinoId,
        UUID sucursalId,
        LocalDate fechaEsperada,
        @PositiveOrZero BigDecimal flete,
        String observaciones,
        @NotEmpty @Valid List<LineaDeSolicitud> lineas) {

    public record LineaDeSolicitud(
            @NotNull UUID productoId,
            /** Ya no se usa: el nombre sale del catálogo de Inventario (HU-174). */
            String nombre,
            @NotNull BigDecimal cantidad,
            @PositiveOrZero BigDecimal costoUnitario,
            @PositiveOrZero BigDecimal descuentoPct,
            /** Ya no se usa: la tarifa de IVA la decide el catálogo (I8, HU-174 C2). */
            @PositiveOrZero BigDecimal impuestoPct) {
    }
}
