package com.regenta.usuarios.aplicacion;

import java.util.UUID;

public record SucursalDelNegocio(
        UUID id,
        String codigo,
        String nombre,
        String direccion,
        String ciudad,
        String telefono,
        boolean esPrincipal,
        boolean activa) {
}
