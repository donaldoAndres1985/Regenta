package com.regenta.comandas.domain;

/** Cómo se arma una cuenta (HU-089): por ítem, en partes iguales, por monto fijo, o única. */
public enum ModoDeDivision {
    UNICA, POR_ITEM, PARTES_IGUALES, MONTO_FIJO;

    public static ModoDeDivision desde(String texto) {
        if (texto == null || texto.isBlank()) {
            return POR_ITEM;
        }
        try {
            return valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException noExiste) {
            return POR_ITEM;
        }
    }
}
