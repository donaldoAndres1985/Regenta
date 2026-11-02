package com.regenta.mesas.domain;

/**
 * El estado de ocupación de una mesa. En HU-081 toda mesa nace {@code LIBRE};
 * las transiciones reales las traen HU-082 (sesión) y HU-084 (plano).
 */
public enum EstadoDeMesa {
    LIBRE, OCUPADA, RESERVADA, CUENTA_PEDIDA, SUCIA, BLOQUEADA
}
