package com.regenta.inventario.application;

import java.math.BigDecimal;
import java.util.UUID;

import com.regenta.inventario.domain.NivelStock;
import com.regenta.inventario.domain.TarifaIva;

/**
 * HU-153 C1 · un producto del inventario general. {@code precio} incluye IVA. {@code costo} y
 * {@code valorCosto} van nulos para quien no tiene INVENTARIO_EXISTENCIA_VER.
 */
public record FilaDeInventario(
        UUID id,
        String sku,
        String nombre,
        String categoria,
        TarifaIva tarifaIva,
        boolean tarifaPorRevisar,
        BigDecimal existencia,
        BigDecimal costo,
        BigDecimal precio,
        BigDecimal valorCosto,
        BigDecimal valorPrecio,
        boolean activo,
        BigDecimal stockMinimo,
        NivelStock nivel) {
}
