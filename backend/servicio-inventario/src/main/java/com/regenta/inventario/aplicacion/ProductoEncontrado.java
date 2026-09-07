package com.regenta.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

import com.regenta.inventario.domain.NivelStock;

/** Una fila del resultado de búsqueda: lo que la lista de inventario muestra. */
public record ProductoEncontrado(
        UUID id,
        String sku,
        String codigoBarras,
        String nombre,
        UUID categoriaId,
        String categoriaNombre,
        BigDecimal precioVenta,
        BigDecimal stockTotal,
        NivelStock nivelStock) {
}
