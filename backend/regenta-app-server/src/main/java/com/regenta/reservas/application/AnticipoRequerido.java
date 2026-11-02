package com.regenta.reservas.application;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Lo que la polÃ­tica de cancelaciÃ³n exige de anticipo para una reserva (HU-070
 * criterio 5). {@code politicaCancelacionId} es la que se aplicÃ³ (la indicada o
 * la de por defecto del negocio); puede ser null si el negocio no tiene ninguna.
 */
public record AnticipoRequerido(UUID politicaCancelacionId, BigDecimal anticipo) {

    public static AnticipoRequerido ninguno() {
        return new AnticipoRequerido(null, BigDecimal.ZERO);
    }
}

