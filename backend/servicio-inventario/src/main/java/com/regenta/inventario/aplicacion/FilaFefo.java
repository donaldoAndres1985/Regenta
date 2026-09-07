package com.regenta.inventario.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Fila cruda de la consulta FEFO: un lote con saldo en una bodega. */
public record FilaFefo(
        UUID loteId,
        String codigoLote,
        LocalDate fechaVencimiento,
        BigDecimal disponible) {
}
