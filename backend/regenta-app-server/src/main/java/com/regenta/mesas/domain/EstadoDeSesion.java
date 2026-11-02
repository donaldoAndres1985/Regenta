package com.regenta.mesas.domain;

/** El ciclo de vida de una sesión de mesa (HU-082). */
public enum EstadoDeSesion {
    ABIERTA, CUENTA_PEDIDA, CERRADA, ANULADA;

    /** Una sesión viva es la que ocupa la mesa: no se puede abrir otra encima. */
    public boolean estaViva() {
        return this == ABIERTA || this == CUENTA_PEDIDA;
    }
}
