package com.regenta.inventario.application;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;

/**
 * HU-156 C3/C5 · los productos que pasan a usar una tabla. Si alguno ya tiene otra, hace falta
 * {@code confirmarCambio}.
 */
public record AsignacionDeTabla(@NotEmpty List<UUID> productoIds, boolean confirmarCambio) {
}
