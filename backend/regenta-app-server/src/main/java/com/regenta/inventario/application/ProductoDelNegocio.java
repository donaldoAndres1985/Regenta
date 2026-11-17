package com.regenta.inventario.application;

import com.regenta.inventario.api.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import com.regenta.inventario.domain.TarifaIva;
import com.regenta.inventario.domain.TipoProducto;

/**
 * Un producto tal como lo ve la app. {@code precioVenta} es el de la etiqueta, con IVA
 * incluido; {@code base} e {@code iva} se calculan de él con la tarifa (HU-147, I2).
 * {@code margenPct} = (base − costo) / costo y {@code beneficioPct} = (base − costo) / base,
 * en porcentaje; nulos sin costo conocido (HU-150).
 */
public record ProductoDelNegocio(
        UUID id,
        String sku,
        String codigoBarras,
        String nombre,
        UUID categoriaId,
        UUID unidadMedidaId,
        TipoProducto tipo,
        BigDecimal precioVenta,
        TarifaIva tarifaIva,
        boolean tarifaPorRevisar,
        BigDecimal base,
        BigDecimal iva,
        BigDecimal costo,
        BigDecimal margenPct,
        BigDecimal beneficioPct,
        boolean manejaLotes,
        boolean perecedero,
        Map<String, Object> atributos,
        boolean activo,
        long version) {
}
