package com.regenta.reportes.aplicacion;

import java.math.BigDecimal;

/**
 * Cómo giró una mesa en un rango, por su código y su zona (HU-134 criterio
 * 4). El código es el snapshot guardado en {@code hechos_comanda} al momento
 * del pedido: sobrevive a que la mesa se elimine después (criterio 5). La
 * zona sale de {@code dim_mesa}, que no está versionada, así que es la zona
 * de hoy.
 */
public record RotacionDeMesa(
        String codigo,
        String zona,
        int comandas,
        int comensales,
        BigDecimal neto,
        BigDecimal minutosMedios) {
}
