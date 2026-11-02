package com.regenta.clientes.domain;

/**
 * Lo que dice el CHECK {@code clientes.tipo_documento}. {@code SIN_IDENTIFICAR}
 * es el «consumidor final»: sin numero de documento y sin unicidad que romper.
 */
public enum TipoDocumento {
    CC,
    CE,
    NIT,
    PP,
    TI,
    NIT_EXT,
    SIN_IDENTIFICAR
}
