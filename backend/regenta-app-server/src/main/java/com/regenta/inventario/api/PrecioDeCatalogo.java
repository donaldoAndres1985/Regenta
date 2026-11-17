package com.regenta.inventario.api;

import java.math.BigDecimal;

/**
 * HU-148 · lo que otro módulo necesita para cobrar un producto: el precio de la etiqueta (con IVA
 * incluido), el código de su tarifa ({@code IVA_19}, {@code IVA_5}, {@code EXENTO},
 * {@code EXCLUIDO}) y su porcentaje.
 */
public record PrecioDeCatalogo(BigDecimal precio, String tarifa, BigDecimal porcentaje) {
}
