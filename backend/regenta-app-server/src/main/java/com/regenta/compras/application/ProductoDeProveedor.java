package com.regenta.compras.application;

import java.math.BigDecimal;
import java.util.UUID;

import com.regenta.compras.domain.ProveedorProducto;
import com.regenta.inventario.api.ProductoResumido;

/**
 * Un producto en el catálogo de un proveedor (HU-046, HU-166). El SKU y el nombre vienen de
 * Inventario; si el producto ya no está en el catálogo del negocio, llegan vacíos.
 */
public record ProductoDeProveedor(
        UUID proveedorId,
        UUID productoId,
        String sku,
        String nombre,
        String codigoProveedor,
        BigDecimal costoUltimo,
        Integer diasEntrega,
        BigDecimal cantidadMinima,
        boolean preferido) {

    static ProductoDeProveedor de(ProveedorProducto pp, ProductoResumido producto) {
        return new ProductoDeProveedor(pp.getProveedorId(), pp.getProductoId(),
                producto == null ? null : producto.sku(),
                producto == null ? null : producto.nombre(),
                pp.getCodigoProveedor(), pp.getCostoUltimo(),
                pp.getDiasEntrega() == null ? null : (int) (short) pp.getDiasEntrega(),
                pp.getCantidadMinima(), pp.isPreferido());
    }
}
