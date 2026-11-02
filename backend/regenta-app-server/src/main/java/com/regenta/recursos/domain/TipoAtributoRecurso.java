package com.regenta.recursos.domain;

/**
 * El tipo de un atributo configurable de un tipo de recurso (HU-064 criterio 1).
 * Misma lista que en Inventario: el CHECK de {@code atributos_tipo_recurso.tipo}
 * la lleva igual.
 */
public enum TipoAtributoRecurso {
    TEXTO,
    NUMERO,
    DECIMAL,
    FECHA,
    BOOLEANO,
    LISTA,
    MULTILISTA;

    public boolean esDeOpciones() {
        return this == LISTA || this == MULTILISTA;
    }
}
