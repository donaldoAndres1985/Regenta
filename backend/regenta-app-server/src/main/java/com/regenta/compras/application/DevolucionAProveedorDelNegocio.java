package com.regenta.compras.application;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.regenta.compras.domain.DevolucionAProveedor;
import com.regenta.compras.domain.LineaDeDevolucionAProveedor;
import com.regenta.compras.domain.NotaCreditoProveedor;

/**
 * HU-178 · Una devolución al proveedor con su nota crédito: cuánto rebajó de la cuenta de la recepción
 * y cuánto quedó a favor del negocio.
 */
public record DevolucionAProveedorDelNegocio(
        UUID id,
        String numero,
        UUID recepcionId,
        UUID proveedorId,
        UUID bodegaId,
        String motivo,
        OffsetDateTime fecha,
        UUID usuarioId,
        BigDecimal valorNota,
        BigDecimal aplicadoACuenta,
        BigDecimal saldoAFavor,
        List<LineaDevuelta> lineas) {

    public record LineaDevuelta(
            UUID recepcionLineaId,
            UUID productoId,
            String nombre,
            BigDecimal cantidad,
            String codigoLote,
            BigDecimal costoEntrada,
            BigDecimal base,
            BigDecimal impuesto) {

        static LineaDevuelta de(LineaDeDevolucionAProveedor l) {
            return new LineaDevuelta(l.getRecepcionLineaId(), l.getProductoId(), l.getNombre(), l.getCantidad(),
                    l.getCodigoLote(), l.getCostoEntrada(), l.getBase(), l.getImpuesto());
        }
    }

    static DevolucionAProveedorDelNegocio de(DevolucionAProveedor d, List<LineaDeDevolucionAProveedor> lineas, NotaCreditoProveedor nota) {
        return new DevolucionAProveedorDelNegocio(d.getId(), d.getNumero(), d.getRecepcionId(), d.getProveedorId(),
                d.getBodegaId(), d.getMotivo(), d.getFecha(), d.getUsuarioId(), d.getValorNota(),
                nota == null ? BigDecimal.ZERO : nota.getAplicadoACuenta(),
                nota == null ? BigDecimal.ZERO : nota.getExcedente(),
                lineas.stream().map(LineaDevuelta::de).toList());
    }
}
