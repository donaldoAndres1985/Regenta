package com.regenta.reservas.domain;

import java.util.Locale;

import com.regenta.comun.errores.ReglaDeNegocioException;

/** Cómo se recibió un pago de reserva. Coincide con el CHECK de {@code pagos_reserva.metodo}. */
public enum MetodoDePago {

    EFECTIVO,
    TARJETA_DEBITO,
    TARJETA_CREDITO,
    TRANSFERENCIA,
    QR,
    OTA,
    OTRO;

    public static MetodoDePago desde(String texto) {
        if (texto == null || texto.isBlank()) {
            return OTRO;
        }
        try {
            return valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException noExiste) {
            throw new ReglaDeNegocioException("Método de pago no válido: " + texto);
        }
    }
}
