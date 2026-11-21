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
    APERTURA,
    /** HU-141: compensa un movimiento manual; apunta al original con {@code anula_a}. */
    ANULACION,
    /**
     * HU-145: salida de efectivo de un pago a proveedor hecho desde la caja abierta. La registra
     * Compras al pagar la cuenta; no es manual, así que no se anula desde la caja.
     */
    PAGO_PROVEEDOR;

    /** I5: solo los movimientos que registra el cajero a mano se pueden anular. */
    public boolean esManual() {
        return this == INGRESO || this == RETIRO || this == GASTO;
    }
}
