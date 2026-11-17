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
        NivelStock nivelStock,
        /** HU-149 C1: la tarifa real, para que el POS no muestre un 0 fijo. */
        com.regenta.inventario.domain.TarifaIva tarifaIva,
        java.math.BigDecimal impuestoPct,
        /** HU-152 C2: ubicación por bodega (solo las bodegas donde se dijo). */
        java.util.Map<UUID, String> ubicaciones) {
}
