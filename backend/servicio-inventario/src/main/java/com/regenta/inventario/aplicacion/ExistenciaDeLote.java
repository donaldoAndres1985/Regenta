package com.regenta.inventario.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** La existencia de un lote, desglosada por bodega. HU-031, criterio 2. */
public record ExistenciaDeLote(
        UUID loteId,
        String codigoLote,
        LocalDate fechaVencimiento,
        boolean vencido,
        List<ExistenciaLoteEnBodega> porBodega,
        BigDecimal total) {
}
