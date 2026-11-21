package com.regenta.compras.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.regenta.compras.domain.Recepcion;
import com.regenta.compras.domain.RecepcionLinea;

/**
 * Una recepción como la ve la app (HU-048, HU-167): la factura del proveedor con su base, IVA,
 * flete y total, y cada línea con su descuento, IVA y lote.
 */
public record RecepcionDelNegocio(
        UUID id,
        String numero,
        UUID ordenId,
        boolean compraDirecta,
        UUID proveedorId,
        UUID bodegaId,
        String estado,
        OffsetDateTime fecha,
        String facturaProveedor,
        LocalDate fechaFactura,
        BigDecimal subtotal,
        BigDecimal descuentoGlobal,
        BigDecimal impuestoTotal,
        BigDecimal flete,
        BigDecimal total,
        String observaciones,
        /** HU-169 · CONTADO o CREDITO, y con qué se pagó si fue de contado. */
        String condicionPago,
        String metodoPago,
        boolean pagarDesdeLaCaja,
        /** HU-169 C4 · Avisos que no impiden registrar (por ejemplo, que se supera el cupo). */
        List<String> avisos,
        /** HU-177 C5 · Quién, cuándo y por qué se anuló; nulos si no. */
        UUID anuladaPor,
        OffsetDateTime anuladaEn,
        String motivoAnulacion,
        List<LineaRecibida> lineas) {

    public record LineaRecibida(
            UUID ordenLineaId,
            UUID productoId,
            String nombre,
            String codigoProveedor,
            BigDecimal cantidad,
            BigDecimal costoUnitario,
            BigDecimal descuentoPct,
            String tarifaIva,
            BigDecimal impuestoPct,
            BigDecimal subtotal,
            BigDecimal impuesto,
            /** HU-168 · Con qué costo unitario entra al inventario (I14). */
            BigDecimal costoEntrada,
            String codigoLote,
            LocalDate fechaVencimiento,
            String registroSanitario) {

        static LineaRecibida de(RecepcionLinea l) {
            return new LineaRecibida(l.getOrdenLineaId(), l.getProductoId(), l.getNombre(),
                    l.getCodigoProveedor(), l.getCantidad(), l.getCostoUnitario(),
                    l.getDescuentoPct(), l.getTarifaIva(), l.getImpuestoPct(), l.subtotal(),
                    l.impuesto(), l.getCostoEntrada(), l.getCodigoLote(), l.getFechaVencimiento(),
                    l.getRegistroSanitario());
        }
    }

    static RecepcionDelNegocio de(Recepcion r, List<RecepcionLinea> lineas) {
        return de(r, lineas, List.of());
    }

    static RecepcionDelNegocio de(Recepcion r, List<RecepcionLinea> lineas, List<String> avisos) {
        return new RecepcionDelNegocio(r.getId(), r.getNumero(), r.getOrdenId(),
                r.esCompraDirecta(), r.getProveedorId(), r.getBodegaId(), r.getEstado().name(),
                r.getFecha(), r.getFacturaProveedor(), r.getFechaFactura(), r.getSubtotal(),
                r.getDescuentoGlobal(), r.getImpuestoTotal(), r.getFlete(), r.getTotal(), r.getObservaciones(),
                r.getCondicionPago().name(), r.getMetodoPago() == null ? null : r.getMetodoPago().name(),
                r.isPagarDesdeLaCaja(), avisos, r.getAnuladaPor(), r.getAnuladaEn(), r.getMotivoAnulacion(),
                lineas.stream().map(LineaRecibida::de).toList());
    }
}
