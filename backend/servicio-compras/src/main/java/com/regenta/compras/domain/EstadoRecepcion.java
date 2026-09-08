package com.regenta.compras.domain;

/**
 * El ciclo de una recepción de mercancía (HU-048).
 *
 * <p>Se arma en {@link #BORRADOR} y al {@link #CONFIRMADA} se sube el inventario
 * (evento {@code recepcion_registrada}) y, si trae factura, se abre la cuenta
 * por pagar. Una recepción confirmada ya no se toca.
 */
public enum EstadoRecepcion {
    BORRADOR,
    CONFIRMADA,
    ANULADA
}
