package com.regenta.caja.domain;

/** Cómo entró el dinero (HU-059/060). Coincide con el CHECK de {@code movimientos_caja}. */
public enum MetodoPagoCaja {
    EFECTIVO,
    TARJETA_DEBITO,
    TARJETA_CREDITO,
    TRANSFERENCIA,
    QR,
    BONO,
    CREDITO,
    OTRO;

    public boolean esEfectivo() {
        return this == EFECTIVO;
    }
}
