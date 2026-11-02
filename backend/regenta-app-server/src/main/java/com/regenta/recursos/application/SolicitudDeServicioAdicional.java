package com.regenta.recursos.application;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Alta o ediciÃ³n de un servicio adicional (HU-068). */
public record SolicitudDeServicioAdicional(
        @NotBlank @Size(max = 30) String codigo,
        @NotBlank @Size(max = 120) String nombre,
        String descripcion,
        @NotNull @PositiveOrZero BigDecimal precio,
        UUID impuestoId,
        String modoCobro,
        UUID productoId) {
}
