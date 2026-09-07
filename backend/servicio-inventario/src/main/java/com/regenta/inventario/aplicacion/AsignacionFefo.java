package com.regenta.inventario.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Un lote sugerido por FEFO y cuanto tomar de el. */
public record AsignacionFefo(
        UUID loteId,
        String codigoLote,
        LocalDate fechaVencimiento,
        BigDecimal cantidadDisponible,
        BigDecimal cantidadSugerida) {
}
