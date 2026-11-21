package com.regenta.inventario.api;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * HU-179 · Lo que Compras necesita para mostrar el margen al recibir: el costo promedio del producto
 * en la bodega, y su precio de venta con IVA incluido con su base sin IVA (Inventario I2).
 */
public record CostoYPrecio(UUID productoId, BigDecimal costoPromedio, BigDecimal precio, BigDecimal base,
        String tarifa) {
}
