package com.regenta.caja.domain;

/** El ciclo de una sesión de caja (HU-059). Coincide con el CHECK de la tabla. */
public enum EstadoSesionCaja {
    ABIERTA,
    CERRADA,
    CUADRADA,
    DESCUADRADA;

    public boolean estaAbierta() {
        return this == ABIERTA;
    }
}
