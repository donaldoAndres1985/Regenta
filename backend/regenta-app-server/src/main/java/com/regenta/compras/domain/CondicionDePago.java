package com.regenta.compras.domain;

/**
 * HU-169 · Cómo se paga la factura de una recepción (I23, I24 de {@code docs/dominio/compras.md}).
 * Coincide con el CHECK de {@code recepciones.condicion_pago}.
 */
public enum CondicionDePago {
    /** Se paga al registrarla: la cuenta nace y queda pagada en la misma transacción. */
    CONTADO,
    /** Nace la cuenta por pagar, que vence según los días de crédito del proveedor. */
    CREDITO
}
