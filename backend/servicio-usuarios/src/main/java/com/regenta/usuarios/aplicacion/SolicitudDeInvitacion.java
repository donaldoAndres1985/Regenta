package com.regenta.usuarios.aplicacion;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** A quien se invita, con que rol y a que sucursales. */
public record SolicitudDeInvitacion(
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 80) String nombre,
        @Size(max = 80) String apellido,
        @NotNull UUID rolId,
        List<UUID> sucursales) {

    public List<UUID> sucursalesOTodas() {
        return sucursales == null ? List.of() : sucursales;
    }
}
