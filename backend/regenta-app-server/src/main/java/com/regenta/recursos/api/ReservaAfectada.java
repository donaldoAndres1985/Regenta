package com.regenta.recursos.api;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Una reserva confirmada que cae dentro de un bloqueo que se acaba de crear
 * (HU-067 criterio 3). El dato lo provee el módulo reservas como advertencia.
 */
public record ReservaAfectada(
        UUID reservaId,
        String codigo,
        OffsetDateTime entrada,
        OffsetDateTime salida,
        String huesped
) {
}
