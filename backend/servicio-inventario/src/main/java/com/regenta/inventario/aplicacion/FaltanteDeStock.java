package com.regenta.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

/** El detalle de qué faltó cuando una reserva no se pudo hacer. */
public record FaltanteDeStock(
        UUID productoId,
        UUID bodegaId,
        BigDecimal solicitado,
        BigDecimal disponible) {
}
