package com.regenta.compras.application;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * HU-167 · Lo que la app necesita para agregar un producto a una compra: la tarifa de IVA y la
 * regla de lote del catálogo, y lo que se sabe de este proveedor (costo y código sugeridos).
 */
public record ProductoParaComprarDelProveedor(
        UUID productoId,
        String sku,
        String nombre,
        String tarifaIva,
        BigDecimal impuestoPct,
        boolean manejaLotes,
        boolean perecedero,
        BigDecimal costoSugerido,
        String codigoProveedor) {
}
