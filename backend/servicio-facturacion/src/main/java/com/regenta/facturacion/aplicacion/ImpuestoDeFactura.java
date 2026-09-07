package com.regenta.facturacion.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

import com.regenta.facturacion.domain.FacturaImpuesto;

/** Un impuesto o retención, para el detalle de la factura. */
public record ImpuestoDeFactura(
        UUID facturaLineaId,
        String codigo,
        String nombre,
        BigDecimal porcentaje,
        BigDecimal base,
        BigDecimal valor,
        boolean esRetencion) {

    static ImpuestoDeFactura de(FacturaImpuesto i) {
        return new ImpuestoDeFactura(i.getFacturaLineaId(), i.getCodigo(), i.getNombre(),
                i.getPorcentaje(), i.getBase(), i.getValor(), i.isEsRetencion());
    }
}
