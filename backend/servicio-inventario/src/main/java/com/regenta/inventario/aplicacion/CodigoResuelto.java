package com.regenta.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * El producto al que apunta un código de barras, con su factor de conversión:
 * 1 para el código propio, N para un alterno (caja de N unidades).
 */
public record CodigoResuelto(
        UUID productoId,
        String sku,
        String nombre,
        BigDecimal factor,
        boolean esAlterno) {
}
