package com.regenta.reservas.domain;

import java.util.Locale;

import com.regenta.comun.errores.ReglaDeNegocioException;

/** Por dónde entró la reserva. Coincide con el CHECK de {@code reservas.canal}. */
public enum CanalReserva {

    MOSTRADOR,
    APP,
    WEB,
    TELEFONO,
    OTA;

    public static CanalReserva desde(String texto) {
        if (texto == null || texto.isBlank()) {
            return MOSTRADOR;
        }
        try {
            return valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException noExiste) {
            throw new ReglaDeNegocioException("Canal de reserva no válido: " + texto);
        }
    }
}
