package com.regenta.inventario.application;

import java.util.UUID;

/**
 * HU-153 · filtros del inventario general. {@code estado}: ACTIVOS (por defecto), INACTIVOS o
 * TODOS. {@code bodegaId} nulo = la suma de todas las bodegas. {@code pagina} empieza en 0.
 */
public record FiltroDeInventario(
        String q,
        UUID categoriaId,
        String estado,
        boolean tarifaPorRevisar,
        boolean existenciaNegativa,
        UUID bodegaId,
        int pagina,
        int tamano) {
}
