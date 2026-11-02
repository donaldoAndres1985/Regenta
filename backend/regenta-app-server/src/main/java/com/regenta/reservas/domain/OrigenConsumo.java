package com.regenta.reservas.domain;

import java.util.Locale;

import com.regenta.shared.web.ReglaDeNegocioException;

/** De dÃ³nde viene un cargo a la habitaciÃ³n. Coincide con el CHECK de {@code consumos_estancia.origen}. */
public enum OrigenConsumo {

    MINIBAR,
    RESTAURANTE,
    SPA,
    LAVANDERIA,
    TELEFONO,
    OTRO;

    public static OrigenConsumo desde(String texto) {
        if (texto == null || texto.isBlank()) {
            return OTRO;
        }
        try {
            return valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException noExiste) {
            throw new ReglaDeNegocioException("Origen de consumo no vÃ¡lido: " + texto);
        }
    }
}

