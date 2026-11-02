package com.regenta.facturacion.domain;

/**
 * De dónde nace una factura. CHECK {@code facturas.origen_tipo}. Es polimórfico
 * (origen_tipo + origen_id), no una FK a `ventas`: con tres patrones,
 * «Facturación depende de Ventas» deja de ser cierto.
 */
public enum OrigenDeFactura {
    VENTA,
    RESERVA,
    COMANDA,
    MANUAL,
    DEVOLUCION
}
