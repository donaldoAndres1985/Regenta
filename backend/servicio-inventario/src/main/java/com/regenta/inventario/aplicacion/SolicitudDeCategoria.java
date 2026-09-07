package com.regenta.inventario.aplicacion;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Alta o edicion de una categoria. El padre es opcional: sin el, es raiz. */
public record SolicitudDeCategoria(
        @NotBlank @Size(max = 100) String nombre,
        UUID categoriaPadreId,
        @Size(max = 2000) String descripcion,
        @Size(max = 40) String icono,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "el color va en formato #RRGGBB")
        @Size(max = 7) String color,
        Integer orden) {
}
