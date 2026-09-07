package com.regenta.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

public record ExistenciaLoteEnBodega(
        UUID bodegaId,
        String bodegaNombre,
        BigDecimal cantidad) {
}
