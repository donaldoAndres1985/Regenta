package com.regenta.inventario.api;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * HU-180 · Lo mínimo para dar de alta un producto que llega por primera vez (I21 de Compras): SKU,
 * nombre, categoría, unidad, tarifa de IVA ({@code IVA_19}, {@code IVA_5}, {@code EXENTO},
 * {@code EXCLUIDO}) y precio con IVA incluido. Inventario aplica sus reglas de siempre.
 */
public record ProductoNuevo(
        String sku,
        String codigoBarras,
        String nombre,
        UUID categoriaId,
        UUID unidadMedidaId,
        String tarifaIva,
        BigDecimal precioVenta,
        boolean manejaLotes,
        boolean perecedero) {
}
