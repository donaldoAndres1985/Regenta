package com.regenta.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

/** El ultimo costo de entrada de un producto (HU-138), como lo lee servicio-menu. */
public record CostoDeProducto(UUID productoId, BigDecimal costoUnitario) {
}
