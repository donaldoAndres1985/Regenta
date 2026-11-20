package com.regenta.inventario.api;

import java.util.UUID;

/** HU-166 · Lo mínimo para mostrar un producto fuera de Inventario: su SKU, nombre y si está activo. */
public record ProductoResumido(UUID productoId, String sku, String nombre, boolean activo) {
}
