package com.regenta.facturacion.aplicacion;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.regenta.facturacion.domain.Factura;
import com.regenta.facturacion.domain.FacturaImpuesto;
import com.regenta.facturacion.domain.FacturaLinea;

/**
 * El detalle de una factura para reimprimir. El emisor y el cliente salen del
 * snapshot congelado en la emisión: no cambian si mañana editan al cliente
 * (HU-053 criterio 6).
 */
public record FacturaDetalle(
        UUID id,
        String numeroCompleto,
        String tipoDocumento,
        String origenTipo,
        UUID origenId,
        String estado,
        OffsetDateTime fechaEmision,
        String moneda,
        BigDecimal subtotal,
        BigDecimal descuentoTotal,
        BigDecimal baseGravable,
        BigDecimal impuestosTotal,
        BigDecimal retencionesTotal,
        BigDecimal propina,
        BigDecimal total,
        String formaPago,
        UUID clienteId,
        Map<String, Object> emisor,
        Map<String, Object> cliente,
        List<LineaDeFactura> lineas,
        List<ImpuestoDeFactura> impuestos) {

    static FacturaDetalle de(Factura f, List<FacturaLinea> lineas, List<FacturaImpuesto> impuestos) {
        return new FacturaDetalle(f.getId(), f.getNumeroCompleto(), "FACTURA_VENTA",
                f.getOrigenTipo().name(), f.getOrigenId(), f.getEstado().name(), f.getFechaEmision(),
                f.getMoneda(), f.getSubtotal(), f.getDescuentoTotal(), f.getBaseGravable(),
                f.getImpuestosTotal(), f.getRetencionesTotal(), f.getPropina(), f.getTotal(),
                f.getFormaPago().name(), f.getClienteId(), f.getEmisorSnapshot(),
                f.getClienteSnapshot(), lineas.stream().map(LineaDeFactura::de).toList(),
                impuestos.stream().map(ImpuestoDeFactura::de).toList());
    }
}
