package com.regenta.inventario.application;

import java.math.BigDecimal;

/**
 * R13 · lo que muestran las tarjetas de Inventario: todo el inventario activo del negocio, sin
 * los filtros de la lista. Así, filtrar o confirmar tarifas no hace parecer que el inventario
 * cambió. {@code valorCosto} va nulo sin INVENTARIO_EXISTENCIA_VER.
 */
public record ResumenDelInventario(
        int productos,
        BigDecimal valorCosto,
        BigDecimal valorPrecio,
        int porRevisar) {
}
