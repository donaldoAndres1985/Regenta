package com.regenta.compras.domain;

/**
 * El ciclo de una orden de compra (HU-047).
 *
 * <p>Solo en {@link #BORRADOR} se editan las líneas; aprobar la mueve a
 * {@link #APROBADA} y a partir de ahí el contenido queda congelado. Los estados
 * de recepción ({@link #PARCIAL}, {@link #RECIBIDA}) los maneja HU-048.
 */
public enum EstadoOrdenCompra {
    BORRADOR,
    APROBADA,
    ENVIADA,
    PARCIAL,
    RECIBIDA,
    CERRADA,
    ANULADA;

    public boolean esBorrador() {
        return this == BORRADOR;
    }
}
