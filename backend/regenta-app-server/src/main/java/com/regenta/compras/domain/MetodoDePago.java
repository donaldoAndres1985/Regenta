package com.regenta.compras.domain;

/** Cómo se le pagó al proveedor (HU-049). Coincide con el CHECK de {@code pagos_proveedor.metodo}. */
public enum MetodoDePago {
    EFECTIVO,
    TRANSFERENCIA,
    CHEQUE,
    TARJETA,
    OTRO
}
