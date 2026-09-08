package com.regenta.alertas.aplicacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Registro de un token FCM del dispositivo del usuario (HU-094). */
public record SolicitudDeDispositivo(
        @NotBlank @Size(max = 255) String tokenFcm,
        String plataforma,
        @Size(max = 80) String modelo,
        @Size(max = 20) String versionApp) {
}
