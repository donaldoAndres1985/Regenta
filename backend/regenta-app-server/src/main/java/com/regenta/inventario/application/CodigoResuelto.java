package com.regenta.inventario.application;

import com.regenta.inventario.api.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * El producto al que apunta un cÃ³digo de barras, con su factor de conversiÃ³n:
 * 1 para el cÃ³digo propio, N para un alterno (caja de N unidades).
 */
public record CodigoResuelto(
        UUID productoId,
        String sku,
        String nombre,
        BigDecimal factor,
        boolean esAlterno) {
}
