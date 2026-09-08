package com.regenta.facturacion.aplicacion;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/** Envío de la factura al cliente. Sin correo se usa el del snapshot del cliente. */
public record SolicitudDeEnvio(@Email @Size(max = 150) String correo) {
}
