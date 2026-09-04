package com.regenta.usuarios.aplicacion;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Un usuario tal como lo lista la pantalla de Usuarios. */
public record UsuarioDelNegocio(
        UUID id,
        String email,
        String nombre,
        String apellido,
        String estado,
        List<UUID> roles,
        List<UUID> sucursales,
        OffsetDateTime ultimoAccesoEn) {
}
