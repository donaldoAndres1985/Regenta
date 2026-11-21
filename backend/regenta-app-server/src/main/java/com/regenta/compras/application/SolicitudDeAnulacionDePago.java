package com.regenta.compras.application;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * HU-172 · Anular un pago con su motivo. Si el pago salió de una caja que ya cerró, {@code sesionCajaId}
 * es la sesión abierta que recibe el reverso; sin ella se usa la caja abierta del usuario.
 */
public record SolicitudDeAnulacionDePago(
        @NotBlank @Size(max = 200) String motivo,
        UUID sesionCajaId) {
}
