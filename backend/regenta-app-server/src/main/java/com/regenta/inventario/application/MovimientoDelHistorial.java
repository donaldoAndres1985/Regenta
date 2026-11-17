package com.regenta.inventario.application;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.regenta.inventario.domain.TipoMovimiento;

/**
 * HU-160 C1 · un movimiento del historial de un producto: {@code cantidad} lleva el signo
 * (salidas en negativo) y {@code saldoPosterior} es lo que quedó en esa bodega.
 */
public record MovimientoDelHistorial(
        UUID id,
        OffsetDateTime ocurridoEn,
        UUID bodegaId,
        TipoMovimiento tipo,
        BigDecimal cantidad,
        BigDecimal saldoPosterior,
        String motivo,
        String usuario) {
}
