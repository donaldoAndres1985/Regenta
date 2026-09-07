package com.regenta.inventario.domain;

/**
 * Cómo está el stock de un producto frente a su mínimo. Alimenta el color de la
 * cantidad en la lista de inventario (HU-035).
 */
public enum NivelStock {
    /** Hay stock y está por encima del mínimo. */
    NORMAL,
    /** Hay stock pero está en el mínimo o por debajo. */
    BAJO,
    /** No hay stock. */
    CERO
}
