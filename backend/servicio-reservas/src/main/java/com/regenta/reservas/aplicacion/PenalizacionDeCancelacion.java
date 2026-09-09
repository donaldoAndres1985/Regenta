package com.regenta.reservas.aplicacion;

import java.math.BigDecimal;

/**
 * Lo que la política de cancelación decide al cancelar una reserva (HU-071).
 * {@code dentroDePlazo} es true si se canceló con la antelación suficiente; en
 * ese caso {@code penalizacion} es cero.
 */
public record PenalizacionDeCancelacion(BigDecimal penalizacion, boolean dentroDePlazo) {

    public static PenalizacionDeCancelacion sinCosto() {
        return new PenalizacionDeCancelacion(BigDecimal.ZERO, true);
    }
}
