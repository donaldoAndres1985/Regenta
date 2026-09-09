package com.regenta.menu.domain;

import java.util.Locale;

import com.regenta.comun.errores.ReglaDeNegocioException;

/** En qué momento de la comida entra el ítem. Coincide con el CHECK de {@code items_menu.curso}. */
public enum CursoDeMenu {

    ENTRADA,
    FUERTE,
    POSTRE,
    BEBIDA,
    ACOMPANAMIENTO;

    public static CursoDeMenu desde(String texto) {
        if (texto == null || texto.isBlank()) {
            return FUERTE;
        }
        try {
            return valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException noExiste) {
            throw new ReglaDeNegocioException("Curso de menú no válido: " + texto);
        }
    }
}
