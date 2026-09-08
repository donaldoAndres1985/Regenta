package com.regenta.compras.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

import com.regenta.compras.domain.ProveedorProducto;

public record ProductoDeProveedor(
        UUID proveedorId,
        UUID productoId,
        String codigoProveedor,
        BigDecimal costoUltimo,
        Integer diasEntrega,
        BigDecimal cantidadMinima,
        boolean preferido) {

    static ProductoDeProveedor de(ProveedorProducto pp) {
        return new ProductoDeProveedor(pp.getProveedorId(), pp.getProductoId(),
                pp.getCodigoProveedor(), pp.getCostoUltimo(),
                pp.getDiasEntrega() == null ? null : (int) (short) pp.getDiasEntrega(),
                pp.getCantidadMinima(), pp.isPreferido());
    }
}
