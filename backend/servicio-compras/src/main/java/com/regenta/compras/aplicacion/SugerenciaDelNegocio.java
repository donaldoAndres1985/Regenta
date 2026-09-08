package com.regenta.compras.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

import com.regenta.compras.domain.SugerenciaDeCompra;

/** Una sugerencia de compra: qué reponer, cuánto y a qué proveedor (HU-050). */
public record SugerenciaDelNegocio(
        UUID id,
        UUID productoId,
        String nombre,
        UUID bodegaId,
        BigDecimal existencia,
        BigDecimal stockMinimo,
        BigDecimal stockObjetivo,
        BigDecimal cantidadSugerida,
        UUID proveedorId,
        BigDecimal costoEstimado,
        boolean sinProveedor,
        String estado) {

    static SugerenciaDelNegocio de(SugerenciaDeCompra s) {
        return new SugerenciaDelNegocio(s.getId(), s.getProductoId(), s.getNombreSnapshot(),
                s.getBodegaId(), s.getExistencia(), s.getStockMinimo(), s.getStockObjetivo(),
                s.getCantidadSugerida(), s.getProveedorId(), s.getCostoEstimado(),
                s.isSinProveedor(), s.getEstado().name());
    }
}
