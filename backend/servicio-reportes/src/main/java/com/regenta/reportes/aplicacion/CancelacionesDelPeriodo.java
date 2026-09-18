package com.regenta.reportes.aplicacion;

import java.math.BigDecimal;

/**
 * Cuánto perdió el hotel en cancelaciones y no-shows en un rango (HU-135
 * criterio 3). {@code totalReservas} son las que llegaron a un estado final
 * en el período: finalizadas, canceladas o no-show.
 */
public record CancelacionesDelPeriodo(
        int totalReservas,
        int canceladas,
        int noShows,
        BigDecimal tasaCancelacion,
        BigDecimal tasaNoShow,
        BigDecimal penalizacionTotal) {
}
