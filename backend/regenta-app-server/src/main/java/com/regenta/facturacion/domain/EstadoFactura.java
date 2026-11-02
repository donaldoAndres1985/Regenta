package com.regenta.facturacion.domain;

/** Ciclo de vida del documento electrónico. CHECK {@code facturas.estado}. */
public enum EstadoFactura {
    BORRADOR,
    GENERADA,
    FIRMADA,
    ENVIADA,
    ACEPTADA,
    RECHAZADA,
    ANULADA,
    CONTINGENCIA
}
