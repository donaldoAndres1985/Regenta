package com.regenta.clientes.domain;

/** De dónde viene una cuenta por cobrar. CHECK {@code cuentas_por_cobrar.origen_tipo}. */
public enum OrigenDeCuenta {
    VENTA,
    RESERVA,
    COMANDA,
    FACTURA
}
