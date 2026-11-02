package com.regenta.inventario.application;

import com.regenta.inventario.api.*;

import java.math.BigDecimal;
import java.util.UUID;

public record ExistenciaLoteEnBodega(
        UUID bodegaId,
        String bodegaNombre,
        BigDecimal cantidad) {
}
