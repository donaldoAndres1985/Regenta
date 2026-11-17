package com.regenta.inventario.api;

import java.math.BigDecimal;

/**
 * HU-158 · una fracción con la que se vende un producto: medida, factor (lo que descuenta del
 * producto) y precio con IVA incluido (el propio del producto si lo tiene, si no el de su tabla).
 */
public record FraccionDeCatalogo(String medida, BigDecimal factor, BigDecimal precio) {
}
