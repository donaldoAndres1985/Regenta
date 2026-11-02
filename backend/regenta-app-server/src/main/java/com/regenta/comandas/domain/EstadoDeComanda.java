package com.regenta.comandas.domain;

/**
 * El ciclo de vida de la cabecera de la comanda. Lo que distingue al patrón
 * Comanda: la transacción queda abierta y acumula líneas durante el servicio.
 */
public enum EstadoDeComanda {
    ABIERTA, EN_COCINA, SERVIDA, CUENTA_PEDIDA, CERRADA, ANULADA;

    /** Una comanda viva todavía admite líneas nuevas (HU-085 criterio 1). */
    public boolean admiteLineas() {
        return this != CERRADA && this != ANULADA;
    }
}
