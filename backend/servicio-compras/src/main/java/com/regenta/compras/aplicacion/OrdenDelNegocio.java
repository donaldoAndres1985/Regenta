package com.regenta.compras.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.regenta.compras.domain.OrdenCompraLinea;
import com.regenta.compras.domain.OrdenDeCompra;

/** Una orden de compra con sus líneas y el avance de recepción (HU-047 criterio 4). */
public record OrdenDelNegocio(
        UUID id,
        String numero,
        UUID proveedorId,
        UUID bodegaDestinoId,
        String estado,
        LocalDate fechaEmision,
        LocalDate fechaEsperada,
        BigDecimal subtotal,
        BigDecimal descuentoTotal,
        BigDecimal impuestoTotal,
        BigDecimal flete,
        BigDecimal total,
        UUID aprobadoPor,
        OffsetDateTime aprobadoEn,
        String observaciones,
        List<LineaDeOrden> lineas) {

    public record LineaDeOrden(
            int linea,
            UUID productoId,
            String nombre,
            BigDecimal cantidadPedida,
            BigDecimal cantidadRecibida,
            BigDecimal faltante,
            BigDecimal costoUnitario,
            BigDecimal descuentoPct,
            BigDecimal impuestoPct,
            BigDecimal subtotal,
            BigDecimal total) {

        static LineaDeOrden de(OrdenCompraLinea l) {
            return new LineaDeOrden(l.getLinea(), l.getProductoId(), l.getNombreSnapshot(),
                    l.getCantidadPedida(), l.getCantidadRecibida(), l.faltante(),
                    l.getCostoUnitario(), l.getDescuentoPct(), l.getImpuestoPct(),
                    l.getSubtotal(), l.getTotal());
        }
    }

    static OrdenDelNegocio de(OrdenDeCompra o, List<OrdenCompraLinea> lineas) {
        return new OrdenDelNegocio(o.getId(), o.getNumero(), o.getProveedorId(),
                o.getBodegaDestinoId(), o.getEstado().name(), o.getFechaEmision(),
                o.getFechaEsperada(), o.getSubtotal(), o.getDescuentoTotal(),
                o.getImpuestoTotal(), o.getFlete(), o.getTotal(), o.getAprobadoPor(),
                o.getAprobadoEn(), o.getObservaciones(),
                lineas.stream().map(LineaDeOrden::de).toList());
    }
}
