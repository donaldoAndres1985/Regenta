package com.regenta.menu.aplicacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Alta o edición de una categoría de la carta (HU-076). */
public record SolicitudDeCategoria(
        @NotBlank @Size(max = 80) String nombre,
        String descripcion,
        @PositiveOrZero Integer orden,
        @Size(max = 40) String icono) {
}
