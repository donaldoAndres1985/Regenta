package com.regenta.reservas.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Un tramo en que un recurso está tomado por una reserva (HU-069). Al comparar
 * contra un periodo consultado se le suma el buffer del tipo de recurso: unos
 * minutos antes para preparar la entrada y unos minutos después para la limpieza
 * (criterio 4). El intervalo es medio abierto, así que un check-out a las 11:00
 * no choca con una consulta que arranca a las 11:00 si el buffer es cero
 * (criterio 3).
 */
public record VentanaOcupada(UUID recursoId, OffsetDateTime desde, OffsetDateTime hasta) {

    /**
     * Esta ventana, ensanchada por el buffer, se solapa con {@code [desde, hasta)}.
     */
    public boolean chocaCon(OffsetDateTime desde, OffsetDateTime hasta, int bufferAntesMin,
            int bufferDespuesMin) {
        OffsetDateTime inicioConBuffer = this.desde.minusMinutes(Math.max(0, bufferAntesMin));
        OffsetDateTime finConBuffer = this.hasta.plusMinutes(Math.max(0, bufferDespuesMin));
        return inicioConBuffer.isBefore(hasta) && finConBuffer.isAfter(desde);
    }
}
