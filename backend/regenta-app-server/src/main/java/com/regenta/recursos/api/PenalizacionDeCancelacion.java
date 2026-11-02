package com.regenta.recursos.api;

import java.math.BigDecimal;

/**
 * Lo que la política de cancelación decide al cancelar una reserva (HU-071).
 */
public record PenalizacionDeCancelacion(BigDecimal penalizacion, boolean dentroDePlazo) {

    public static PenalizacionDeCancelacion sinCosto() {
        return new PenalizacionDeCancelacion(BigDecimal.ZERO, true);
    }
}
