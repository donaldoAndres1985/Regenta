package com.regenta.inventario.domain;

/**
 * Los tipos que puede tener un atributo de categoria (HU-027, criterio 1). El
 * CHECK de {@code atributos_categoria.tipo} lleva exactamente esta lista.
 */
public enum TipoAtributo {
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

    public boolean esNumerico() {
        return this == NUMERO || this == DECIMAL;
    }
}
