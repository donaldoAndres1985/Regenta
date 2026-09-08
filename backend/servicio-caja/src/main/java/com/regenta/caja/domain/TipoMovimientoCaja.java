package com.regenta.caja.domain;

/** El motivo de un movimiento de caja (HU-059/060). Coincide con el CHECK de la tabla. */
public enum TipoMovimientoCaja {
    VENTA,
    COMANDA,
    RESERVA,
    DEVOLUCION,
    RECAUDO,
    INGRESO,
    RETIRO,
    GASTO,
    AJUSTE,
    APERTURA
}
