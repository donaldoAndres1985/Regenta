package com.regenta.compras.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Un proveedor que vende un producto, con su costo. Ordenados con el preferido
 * primero (HU-046 criterio 3). Lo consumen HU-047 (sugerir costos) y HU-050
 * (agrupar la sugerencia de compra).
 */
public record ProveedorDeProducto(
        UUID proveedorId,
        String razonSocial,
        String codigoProveedor,
        BigDecimal costoUltimo,
        Integer diasEntrega,
        BigDecimal cantidadMinima,
        boolean preferido,
        int diasCredito) {
}
