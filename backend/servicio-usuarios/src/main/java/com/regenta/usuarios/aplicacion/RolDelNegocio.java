package com.regenta.usuarios.aplicacion;

import java.util.List;
import java.util.UUID;

public record RolDelNegocio(
        UUID id,
        String nombre,
        String descripcion,
        boolean esSistema,
        boolean activo,
        List<String> permisos,
        long usuariosAsignados) {
}
