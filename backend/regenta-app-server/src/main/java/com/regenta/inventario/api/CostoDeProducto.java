package com.regenta.inventario.api;

import java.math.BigDecimal;
import java.util.UUID;

/** El último costo de entrada de un producto (HU-138), como lo lee menú. */
public record CostoDeProducto(UUID productoId, BigDecimal costoUnitario) {
}
