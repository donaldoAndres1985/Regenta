package com.regenta.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

/** El precio que aplica para un producto en una lista, dada una cantidad. */
public record PrecioResuelto(
        UUID productoId,
        UUID listaId,
        BigDecimal precio,
        BigDecimal cantidadMinimaAplicada,
        BigDecimal descuentoMaxPct) {
}
