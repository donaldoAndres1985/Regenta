package com.regenta.inventario.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Una fila de la consulta de lotes por vencer, antes de calcular los días. */
public record FilaPorVencer(
        UUID loteId,
        UUID productoId,
        String productoNombre,
        String codigoLote,
        LocalDate fechaVencimiento,
        BigDecimal existencia) {
}
