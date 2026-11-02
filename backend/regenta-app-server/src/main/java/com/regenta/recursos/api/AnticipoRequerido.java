package com.regenta.recursos.api;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Lo que la política de cancelación exige de anticipo para una reserva (HU-070 criterio 5).
 */
public record AnticipoRequerido(UUID politicaCancelacionId, BigDecimal anticipo) {

    public static AnticipoRequerido ninguno() {
        return new AnticipoRequerido(null, BigDecimal.ZERO);
    }
}
