package com.regenta.inventario.application;

import com.regenta.inventario.api.*;

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
