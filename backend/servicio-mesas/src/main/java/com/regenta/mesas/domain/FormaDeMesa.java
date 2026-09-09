package com.regenta.mesas.domain;

import com.regenta.comun.errores.ReglaDeNegocioException;

/** La forma de la mesa en el plano del salón (HU-081). */
public enum FormaDeMesa {
    CUADRADA, REDONDA, RECTANGULAR, BARRA;

    public static FormaDeMesa desde(String texto) {
        if (texto == null || texto.isBlank()) {
            return CUADRADA;
        }
        try {
            return valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException noExiste) {
            throw new ReglaDeNegocioException("Forma de mesa no válida: " + texto);
        }
    }
}
