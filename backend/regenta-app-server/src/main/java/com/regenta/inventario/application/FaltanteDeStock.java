package com.regenta.inventario.application;

import com.regenta.inventario.api.*;

import java.math.BigDecimal;
import java.util.UUID;

/** El detalle de quÃ© faltÃ³ cuando una reserva no se pudo hacer. */
public record FaltanteDeStock(
        UUID productoId,
        UUID bodegaId,
        BigDecimal solicitado,
        BigDecimal disponible) {
}
