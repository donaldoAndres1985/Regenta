package com.regenta.usuarios.aplicacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AceptacionDeInvitacion(
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 72) String password) {
}
