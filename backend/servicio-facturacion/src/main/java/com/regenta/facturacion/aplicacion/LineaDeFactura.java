package com.regenta.facturacion.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

import com.regenta.facturacion.domain.FacturaLinea;

/** Una línea, para el detalle de la factura. */
public record LineaDeFactura(
        UUID id,
        int linea,
        String codigo,
        String descripcion,
        BigDecimal cantidad,
        String unidadCodigo,
        BigDecimal precioUnitario,
        BigDecimal descuentoValor,
        BigDecimal baseGravable,
        BigDecimal total) {

    static LineaDeFactura de(FacturaLinea l) {
        return new LineaDeFactura(l.getId(), l.getLinea(), l.getCodigo(), l.getDescripcion(),
                l.getCantidad(), l.getUnidadCodigo(), l.getPrecioUnitario(), l.getDescuentoValor(),
                l.getBaseGravable(), l.getTotal());
    }
}
