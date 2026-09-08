package com.regenta.caja.aplicacion;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Alta de un punto de cobro (HU-059). */
public record SolicitudDeCaja(
        @NotBlank @Size(max = 20) String codigo,
        @NotBlank @Size(max = 60) String nombre,
        @Size(max = 80) String terminalId,
        UUID sucursalId) {
}
