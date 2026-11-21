package com.regenta.compras.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.regenta.compras.domain.OrdenCompraLinea;
import com.regenta.compras.domain.OrdenDeCompra;

/**
 * Una orden de compra con sus líneas y el avance de recepción (HU-047 C4, HU-173, HU-174): de cada
 * línea, lo pedido, lo recibido y lo que falta, con el código con que la vende el proveedor.
 */
public record OrdenDelNegocio(
        UUID id,
        String numero,
        UUID proveedorId,
        /** HU-174 · Para el listado, sin otra consulta. */
        String proveedorNombre,
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
        /** HU-173 · Se aprobó sola al confirmarla. */
        boolean aprobacionAutomatica,
        /** HU-173 · Confirmada, esperando que alguien con permiso la apruebe. */
        boolean esperaAprobacion,
        String observaciones,
        /** HU-176 · Quién, cuándo y por qué se anuló; nulos si no. */
        UUID anuladaPor,
        OffsetDateTime anuladaEn,
        String motivoAnulacion,
        /** HU-176 · Quién, cuándo y por qué se cerró con faltante; nulos si no. */
        UUID cerradaPor,
        OffsetDateTime cerradaEn,
        String motivoCierre,
        List<LineaDeOrden> lineas) {

    public record LineaDeOrden(
            UUID id,
            int linea,
            UUID productoId,
            String nombre,
            /** HU-174 · Con qué código lo vende el proveedor (de su catálogo), si se sabe. */
            String codigoProveedor,
            BigDecimal cantidadPedida,
            BigDecimal cantidadRecibida,
            BigDecimal faltante,
            BigDecimal costoUnitario,
            BigDecimal descuentoPct,
            BigDecimal impuestoPct,
            BigDecimal subtotal,
            BigDecimal total) {

        static LineaDeOrden de(OrdenCompraLinea l, String codigoProveedor) {
            return new LineaDeOrden(l.getId(), l.getLinea(), l.getProductoId(), l.getNombreSnapshot(),
                    codigoProveedor, l.getCantidadPedida(), l.getCantidadRecibida(), l.faltante(),
                    l.getCostoUnitario(), l.getDescuentoPct(), l.getImpuestoPct(), l.getSubtotal(), l.getTotal());
        }
    }

    static OrdenDelNegocio de(OrdenDeCompra o, List<OrdenCompraLinea> lineas) {
        return de(o, lineas, null, Map.of());
    }

    static OrdenDelNegocio de(OrdenDeCompra o, List<OrdenCompraLinea> lineas, String proveedorNombre,
            Map<UUID, String> codigosDelProveedor) {
        return new OrdenDelNegocio(o.getId(), o.getNumero(), o.getProveedorId(), proveedorNombre,
                o.getBodegaDestinoId(), o.getEstado().name(), o.getFechaEmision(), o.getFechaEsperada(),
                o.getSubtotal(), o.getDescuentoTotal(), o.getImpuestoTotal(), o.getFlete(), o.getTotal(),
                o.getAprobadoPor(), o.getAprobadoEn(), o.isAprobacionAutomatica(), o.esperaAprobacion(),
                o.getObservaciones(), o.getAnuladaPor(), o.getAnuladaEn(), o.getMotivoAnulacion(),
                o.getCerradaPor(), o.getCerradaEn(), o.getMotivoCierre(),
                lineas.stream().map(l -> LineaDeOrden.de(l, codigosDelProveedor.get(l.getProductoId()))).toList());
    }
}
