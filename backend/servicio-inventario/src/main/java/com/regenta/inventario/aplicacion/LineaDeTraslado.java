package com.regenta.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Una linea del traslado. {@code codigoLote} es obligatorio si el producto
 * maneja lotes; para el resto se deja vacio.
 */
public record LineaDeTraslado(
        @NotNull UUID productoId,
        @Size(max = 60) String codigoLote,
        @NotNull @Positive BigDecimal cantidad) {
}
