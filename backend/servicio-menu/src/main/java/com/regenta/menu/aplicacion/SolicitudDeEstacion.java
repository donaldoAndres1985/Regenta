package com.regenta.menu.aplicacion;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Alta o edición de una estación de cocina (HU-077). */
public record SolicitudDeEstacion(
        @NotBlank @Size(max = 20) String codigo,
        @NotBlank @Size(max = 60) String nombre,
        @Size(max = 80) String impresora,
        @PositiveOrZero Integer orden,
        UUID sucursalId) {
}
