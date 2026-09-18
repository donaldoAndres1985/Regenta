package com.regenta.reportes.aplicacion;

import java.math.BigDecimal;

/** HU-098 criterio 1: monto, unidades y margen de una categoría en un rango de fechas. */
public record VentaPorCategoria(String categoria, BigDecimal monto, BigDecimal unidades, BigDecimal margen) {
}
