package com.regenta.inventario.application;

import com.regenta.inventario.api.*;

import java.math.BigDecimal;
import java.util.UUID;

public record LineaDelTraslado(
        UUID id,
        UUID productoId,
        UUID loteId,
        BigDecimal cantidadEnviada,
        BigDecimal cantidadRecibida) {
}
