package com.regenta.recursos.domain;

import java.util.Locale;

import com.regenta.comun.errores.ReglaDeNegocioException;

/** Por qué un recurso queda fuera de servicio un periodo (HU-067). */
public enum MotivoBloqueo {

    MANTENIMIENTO,
    LIMPIEZA,
    EVENTO,
    FERIADO,
    OTRO;

    public static MotivoBloqueo desde(String texto) {
        if (texto == null || texto.isBlank()) {
            return OTRO;
        }
        try {
            return valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException noExiste) {
            throw new ReglaDeNegocioException("Motivo de bloqueo no válido: " + texto);
        }
    }
}
