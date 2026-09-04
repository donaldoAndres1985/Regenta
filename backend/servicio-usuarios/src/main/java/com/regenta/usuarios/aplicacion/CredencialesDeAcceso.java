package com.regenta.usuarios.aplicacion;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Lo que manda la app para entrar. El negocio es opcional: solo hace falta
 * cuando el mismo correo trabaja en mas de uno.
 */
public record CredencialesDeAcceso(
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 200) String password,
        UUID negocioId,
        @Size(max = 80) String dispositivoId,
        @Size(max = 20) String plataforma) {
}
