package com.regenta.reservas.aplicacion;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Una reserva tal como se pinta en el calendario (HU-075): una barra por recurso y periodo. */
public record ReservaEnCalendario(
        UUID id,
        UUID recursoId,
        String numero,
        String estado,
        OffsetDateTime desde,
        OffsetDateTime hasta) {
}
