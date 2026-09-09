package com.regenta.menu.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

/** Un producto de inventario y cuánto se consume, tras explotar líneas de comanda (HU-079). */
public record InsumoAConsumir(UUID productoId, BigDecimal cantidad, String nombre) {
}
