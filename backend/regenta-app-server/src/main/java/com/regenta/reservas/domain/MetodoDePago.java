package com.regenta.reservas.domain;

import java.util.Locale;

import com.regenta.shared.web.ReglaDeNegocioException;

/** CÃ³mo se recibiÃ³ un pago de reserva. Coincide con el CHECK de {@code pagos_reserva.metodo}. */
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
            throw new ReglaDeNegocioException("MÃ©todo de pago no vÃ¡lido: " + texto);
        }
    }
}

