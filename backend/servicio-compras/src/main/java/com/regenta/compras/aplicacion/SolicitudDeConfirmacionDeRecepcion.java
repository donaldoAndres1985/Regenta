package com.regenta.compras.aplicacion;

/** Al confirmar la recepción se puede adjuntar (o corregir) la factura del proveedor. */
public record SolicitudDeConfirmacionDeRecepcion(String facturaProveedor) {
}
