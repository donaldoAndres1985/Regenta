package com.regenta.facturacion.aplicacion;

import java.util.UUID;

/** Cómo quedó una transmisión: el estado de la factura y, si hubo, el rechazo. */
public record ResultadoDeTransmision(
        UUID facturaId,
        String estado,
        boolean aceptada,
        String codigoError,
        int intentos) {
}
