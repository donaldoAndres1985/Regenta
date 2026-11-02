package com.regenta.ventas.domain;

/** El CHECK de {@code ventas.estado} lleva esta lista. */
public enum EstadoVenta {
    BORRADOR,
    PENDIENTE_STOCK,
    CONFIRMADA,
    DEVUELTA_PARCIAL,
    DEVUELTA,
    ANULADA
}
