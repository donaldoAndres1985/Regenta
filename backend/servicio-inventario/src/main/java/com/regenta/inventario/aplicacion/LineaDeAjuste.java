package com.regenta.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Una línea del ajuste: el producto y lo que se contó de verdad. La cantidad de
 * sistema la toma el servicio al cargar el ajuste; aquí solo va la física.
 */
public record LineaDeAjuste(
        @NotNull UUID productoId,
        @Size(max = 60) String codigoLote,
        @NotNull @PositiveOrZero BigDecimal cantidadFisica,
        BigDecimal costoUnitario) {
}
