package com.regenta.compras.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Lo que se pide para armar una orden de compra (HU-047). El costo de cada línea
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
            @NotNull String nombre,
            @NotNull BigDecimal cantidad,
            @PositiveOrZero BigDecimal costoUnitario,
            @PositiveOrZero BigDecimal descuentoPct,
            @PositiveOrZero BigDecimal impuestoPct) {
    }
}
