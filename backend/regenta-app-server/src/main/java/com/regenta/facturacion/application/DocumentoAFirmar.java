package com.regenta.facturacion.application;

import java.util.List;

import com.regenta.facturacion.domain.Factura;
import com.regenta.facturacion.domain.FacturaImpuesto;
import com.regenta.facturacion.domain.FacturaLinea;
import com.regenta.facturacion.domain.Resolucion;

/**
 * HU-117 · Todo lo que entra al XML UBL de una factura: la factura (con los snapshots de emisor y
 * adquiriente), sus líneas e impuestos, y la resolución que autorizó el número (con su clave técnica,
 * que entra al CUFE).
 */
public record DocumentoAFirmar(
        Factura factura,
        List<FacturaLinea> lineas,
        List<FacturaImpuesto> impuestos,
        Resolucion resolucion) {
}
