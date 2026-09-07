package com.regenta.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

public record ExistenciaEnBodega(
        UUID bodegaId,
        String bodegaNombre,
        BigDecimal cantidad,
        BigDecimal cantidadReservada,
        BigDecimal cantidadDisponible) {
}
