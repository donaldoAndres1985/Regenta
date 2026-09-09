package com.regenta.mesas.aplicacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Alta o edición de una zona (HU-081). */
public record SolicitudDeZona(
        @NotBlank @Size(max = 60) String nombre,
        Integer orden,
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "El color va en formato #RRGGBB")
        String color) {
}
