package com.regenta.inventario.application;

import com.regenta.inventario.api.*;

import java.math.BigDecimal;
import java.util.UUID;

public record LineaDelAjuste(
        UUID id,
        UUID productoId,
        UUID loteId,
        BigDecimal cantidadSistema,
        BigDecimal cantidadFisica,
        BigDecimal diferencia) {
}
