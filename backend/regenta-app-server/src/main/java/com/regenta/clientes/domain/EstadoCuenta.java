package com.regenta.clientes.domain;

/** Lo que dice el CHECK {@code cuentas_por_cobrar.estado}. */
public enum EstadoCuenta {
    PENDIENTE,
    PARCIAL,
    PAGADA,
    VENCIDA,
    INCOBRABLE
}
