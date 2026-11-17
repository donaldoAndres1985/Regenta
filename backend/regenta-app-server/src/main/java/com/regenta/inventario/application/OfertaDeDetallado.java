package com.regenta.inventario.application;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * HU-163 · lo que el POS ofrece cuando falta un producto: abrir 1 {@code origen} para obtener
 * {@code factor} {@code destino}; hay {@code disponibles} del origen en la bodega.
 */
public record OfertaDeDetallado(UUID origenId, String origen, UUID destinoId, String destino,
        BigDecimal factor, BigDecimal disponibles) {
}
