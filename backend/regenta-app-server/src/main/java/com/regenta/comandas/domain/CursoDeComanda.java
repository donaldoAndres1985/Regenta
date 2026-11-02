package com.regenta.comandas.domain;

/** El curso de una línea: para que la cocina saque los postres al final. */
public enum CursoDeComanda {
    ENTRADA, FUERTE, POSTRE, BEBIDA, ACOMPANAMIENTO;

    public static CursoDeComanda desde(String texto) {
        if (texto == null || texto.isBlank()) {
            return FUERTE;
        }
        try {
            return valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException noExiste) {
            return FUERTE;
        }
    }
}
