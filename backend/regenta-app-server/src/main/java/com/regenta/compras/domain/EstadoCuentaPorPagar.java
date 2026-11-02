package com.regenta.compras.domain;

/** Estado de una cuenta por pagar a un proveedor (HU-048 la abre, HU-049 la paga). */
public enum EstadoCuentaPorPagar {
    PENDIENTE,
    PARCIAL,
    PAGADA,
    VENCIDA,
    ANULADA
}
