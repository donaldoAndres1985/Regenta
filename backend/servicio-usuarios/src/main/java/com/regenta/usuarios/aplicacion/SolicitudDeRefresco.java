package com.regenta.usuarios.aplicacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudDeRefresco(
        @NotBlank String tokenDeRefresco,
        @Size(max = 80) String dispositivoId,
        @Size(max = 20) String plataforma) {
}
