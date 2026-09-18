package com.regenta.reportes.aplicacion;

import java.time.LocalDate;
import java.util.UUID;

/**
 * HU-098 criterio 3. {@code ultimoMovimiento} y {@code diasSinMovimiento} nulos
 * significan que el producto nunca tuvo un movimiento de inventario registrado.
 */
public record RotacionProducto(UUID productoId, String nombre, LocalDate ultimoMovimiento,
        Long diasSinMovimiento) {
}
