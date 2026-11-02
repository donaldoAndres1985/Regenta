package com.regenta.inventario.application;

import com.regenta.inventario.api.*;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Una lÃ­nea del ajuste: el producto y lo que se contÃ³ de verdad. La cantidad de
 * sistema la toma el servicio al cargar el ajuste; aquÃ­ solo va la fÃ­sica.
 */
public record LineaDeAjuste(
        @NotNull UUID productoId,
        @Size(max = 60) String codigoLote,
        @NotNull @PositiveOrZero BigDecimal cantidadFisica,
        BigDecimal costoUnitario) {
}
