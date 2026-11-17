package com.regenta.inventario.application;

import java.math.BigDecimal;

/**
 * HU-156/157 · una fracción tal como se vende: medida, factor, precio con IVA incluido y de dónde
 * sale el precio ({@code TABLA} o {@code PROPIO}, I14).
 */
public record FraccionDelNegocio(String medida, BigDecimal factor, BigDecimal precio, String origen) {
}
