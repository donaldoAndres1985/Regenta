package com.regenta.inventario.api;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * HU-167 · Lo que Compras necesita de un producto para registrarlo en una factura: su tarifa de
 * IVA (la decide el catálogo, no quien compra) y si exige lote o vencimiento al recibirlo.
 */
public record ProductoParaComprar(
        UUID productoId,
        String sku,
        String nombre,
        boolean activo,
        String tarifaIva,
        BigDecimal impuestoPct,
        boolean manejaLotes,
        boolean perecedero) {
}
