package com.regenta.inventario.api;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * HU-177 · Lo que Compras va a sacar de una bodega al anular una recepción: el producto, cuánto y,
 * si entró con lote, de qué lote. Inventario dice si alcanza antes de que Compras anule.
 */
public record SalidaPrevista(UUID productoId, BigDecimal cantidad, String codigoLote) {
}
