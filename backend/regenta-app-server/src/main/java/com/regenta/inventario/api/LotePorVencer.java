package com.regenta.inventario.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Un lote con saldo que vence pronto (HU-138), como lo lee el barrido de
 * vencimientos de alertas (HU-093). {@code existencia} es la suma de
 * todas las bodegas.
 */
public record LotePorVencer(
        UUID loteId,
        UUID productoId,
        String productoNombre,
        String codigoLote,
        LocalDate fechaVencimiento,
        int diasParaVencer,
        BigDecimal existencia) {
}
