package com.regenta.usuarios.aplicacion;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/** Un rol del negocio: un nombre y un subconjunto del catalogo de permisos. */
public record SolicitudDeRol(
        @NotBlank @Size(max = 60) String nombre,
        String descripcion,
        @NotEmpty List<String> permisos) {
}
