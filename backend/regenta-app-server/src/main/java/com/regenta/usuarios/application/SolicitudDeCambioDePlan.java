package com.regenta.usuarios.application;

import jakarta.validation.constraints.NotBlank;

public record SolicitudDeCambioDePlan(@NotBlank String planCodigo, String motivo) {
}
