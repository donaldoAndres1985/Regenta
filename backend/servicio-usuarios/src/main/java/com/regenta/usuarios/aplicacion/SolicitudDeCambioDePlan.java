package com.regenta.usuarios.aplicacion;

import jakarta.validation.constraints.NotBlank;

public record SolicitudDeCambioDePlan(@NotBlank String planCodigo, String motivo) {
}
