package com.regenta.compras.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * HU-178 · Qué se le devuelve al proveedor de una recepción confirmada. Cada línea dice de qué línea
 * de la recepción sale; esa línea fija el producto, el lote (C6) y el costo de entrada.
 */
public record SolicitudDeDevolucionAProveedor(
        @NotNull UUID recepcionId,
        @NotBlank @Size(max = 200) String motivo,
        @NotEmpty @Valid List<Linea> lineas) {

    public record Linea(@NotNull UUID recepcionLineaId, @NotNull @Positive BigDecimal cantidad) {
    }
}
