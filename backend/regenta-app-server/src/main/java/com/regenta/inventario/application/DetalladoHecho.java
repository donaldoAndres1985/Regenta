package com.regenta.inventario.application;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * HU-162 · lo que hizo un detallado: salieron {@code sale} del origen, entraron {@code entra} al
 * destino y el destino quedó con {@code costoDestino} de costo promedio en esa bodega.
 */
public record DetalladoHecho(UUID detalladoId, UUID origenId, String origen, BigDecimal sale,
        UUID destinoId, String destino, BigDecimal entra, BigDecimal costoDestino) {
}
