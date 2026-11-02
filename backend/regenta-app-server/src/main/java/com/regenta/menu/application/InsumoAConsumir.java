package com.regenta.menu.application;

import com.regenta.menu.api.*;

import java.math.BigDecimal;
import java.util.UUID;

/** Un producto de inventario y cuÃ¡nto se consume, tras explotar lÃ­neas de comanda (HU-079). */
public record InsumoAConsumir(UUID productoId, BigDecimal cantidad, String nombre) {
}
