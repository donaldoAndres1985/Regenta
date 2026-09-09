package com.regenta.menu.aplicacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Alta o edición de un grupo de modificadores (HU-078). */
public record SolicitudDeGrupo(
        @NotBlank @Size(max = 80) String nombre,
        @PositiveOrZero Integer minSelecciones,
        @PositiveOrZero Integer maxSelecciones) {
}
