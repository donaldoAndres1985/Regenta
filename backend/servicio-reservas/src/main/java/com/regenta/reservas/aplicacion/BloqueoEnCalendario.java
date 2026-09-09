package com.regenta.reservas.aplicacion;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Un bloqueo de recurso tal como se pinta en el calendario (HU-075). */
public record BloqueoEnCalendario(
        UUID recursoId,
        OffsetDateTime desde,
        OffsetDateTime hasta,
        String motivo) {
}
