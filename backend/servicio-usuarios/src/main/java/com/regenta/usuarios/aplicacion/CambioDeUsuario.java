package com.regenta.usuarios.aplicacion;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Lo que un administrador puede cambiarle a otro usuario. La clave, no. */
public record CambioDeUsuario(
        @NotBlank @Size(max = 80) String nombre,
        @Size(max = 80) String apellido,
        @Size(max = 30) String documento,
        @Size(max = 30) String telefono,
        List<UUID> roles,
        List<UUID> sucursales) {
}
