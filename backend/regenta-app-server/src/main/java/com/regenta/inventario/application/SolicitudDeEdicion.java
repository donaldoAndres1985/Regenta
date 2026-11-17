package com.regenta.inventario.application;

import java.math.BigDecimal;
import java.util.UUID;

import com.regenta.inventario.domain.TarifaIva;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * HU-150 · lo que se guarda desde la ficha. {@code precioVenta} es el de la etiqueta, con IVA
 * incluido (I2). {@code version} es la que se leyó: si alguien guardó antes, se rechaza (C5).
 */
public record SolicitudDeEdicion(
        @NotBlank @Size(max = 60) String sku,
        @Size(max = 60) String codigoBarras,
        @NotBlank @Size(max = 180) String nombre,
        @NotNull UUID categoriaId,
        @NotNull UUID unidadMedidaId,
        TarifaIva tarifaIva,
        @NotNull @PositiveOrZero BigDecimal precioVenta,
        @PositiveOrZero BigDecimal costo,
        long version) {
}
