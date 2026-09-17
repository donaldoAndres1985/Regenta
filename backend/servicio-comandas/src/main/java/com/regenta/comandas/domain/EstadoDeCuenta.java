package com.regenta.comandas.domain;

/** El ciclo de vida de una cuenta (HU-089): abierta mientras se le arman líneas, pagada al cobrarla. */
public enum EstadoDeCuenta {
    ABIERTA, PAGADA, ANULADA
}
