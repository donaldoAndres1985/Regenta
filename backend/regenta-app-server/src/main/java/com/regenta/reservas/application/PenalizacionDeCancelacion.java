package com.regenta.reservas.application;

import java.math.BigDecimal;

/**
 * Lo que la polÃ­tica de cancelaciÃ³n decide al cancelar una reserva (HU-071).
 * {@code dentroDePlazo} es true si se cancelÃ³ con la antelaciÃ³n suficiente; en
 * ese caso {@code penalizacion} es cero.
 */
public record PenalizacionDeCancelacion(BigDecimal penalizacion, boolean dentroDePlazo) {

    public static PenalizacionDeCancelacion sinCosto() {
        return new PenalizacionDeCancelacion(BigDecimal.ZERO, true);
    }
}

