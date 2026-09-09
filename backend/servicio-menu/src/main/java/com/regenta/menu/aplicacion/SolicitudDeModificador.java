package com.regenta.menu.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Alta o edición de un modificador de un grupo (HU-078). */
public record SolicitudDeModificador(
        @NotBlank @Size(max = 80) String nombre,
        @PositiveOrZero BigDecimal precioExtra,
        UUID productoId,
        @PositiveOrZero BigDecimal cantidadInsumo,
        @PositiveOrZero Integer orden) {
}
