package com.regenta.inventario.application;

import com.regenta.inventario.api.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Una fila de la consulta de lotes por vencer, antes de calcular los dÃ­as. */
public record FilaPorVencer(
        UUID loteId,
        UUID productoId,
        String productoNombre,
        String codigoLote,
        LocalDate fechaVencimiento,
        BigDecimal existencia) {
}
