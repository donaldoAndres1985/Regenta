package com.regenta.usuarios.aplicacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudDeSucursal(
        @NotBlank @Size(max = 20) String codigo,
        @NotBlank @Size(max = 120) String nombre,
        @Size(max = 200) String direccion,
        @Size(max = 80) String ciudad,
        @Size(max = 30) String telefono,
        boolean principal) {
}
