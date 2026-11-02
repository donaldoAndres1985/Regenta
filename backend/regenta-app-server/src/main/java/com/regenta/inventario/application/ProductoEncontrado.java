package com.regenta.inventario.application;

import com.regenta.inventario.api.*;

import java.math.BigDecimal;
import java.util.UUID;

import com.regenta.inventario.domain.NivelStock;

/** Una fila del resultado de bÃºsqueda: lo que la lista de inventario muestra. */
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
