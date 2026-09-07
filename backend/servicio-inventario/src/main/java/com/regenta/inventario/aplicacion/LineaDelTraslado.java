package com.regenta.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

public record LineaDelTraslado(
        UUID id,
        UUID productoId,
        UUID loteId,
        BigDecimal cantidadEnviada,
        BigDecimal cantidadRecibida) {
}
