package com.regenta.facturacion.domain;

/** CHECK {@code transmisiones.evento}: qué intercambio con la DIAN se registró. */
public enum EventoDeTransmision {
    ENVIO,
    CONSULTA,
    ANULACION,
    ACUSE,
    EMAIL_CLIENTE
}
