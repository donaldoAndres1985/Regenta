package com.regenta.ventas.application;

import java.math.BigDecimal;

/** HU-148 C6 · base e IVA de una venta agrupados por tarifa, en el orden en que aparecen. */
public record ImpuestoPorTarifa(String codigo, BigDecimal porcentaje, BigDecimal base, BigDecimal iva) {
}
