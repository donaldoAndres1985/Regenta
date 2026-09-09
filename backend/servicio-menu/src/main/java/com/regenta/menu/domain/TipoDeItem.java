package com.regenta.menu.domain;

import java.util.Locale;

import com.regenta.comun.errores.ReglaDeNegocioException;

/** Qué es un ítem de la carta. Coincide con el CHECK de {@code items_menu.tipo}. */
public enum TipoDeItem {

    PLATO,
    BEBIDA,
    POSTRE,
    COMBO,
    ADICIONAL;

    public static TipoDeItem desde(String texto) {
        if (texto == null || texto.isBlank()) {
            return PLATO;
        }
        try {
            return valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException noExiste) {
            throw new ReglaDeNegocioException("Tipo de ítem no válido: " + texto);
        }
    }
}
