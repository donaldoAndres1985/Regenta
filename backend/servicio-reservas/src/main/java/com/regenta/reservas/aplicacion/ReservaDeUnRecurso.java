package com.regenta.reservas.aplicacion;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Una reserva vista desde su recurso (HU-138): lo que servicio-recursos necesita
 * para no borrar un recurso con reservas por delante y para avisar qué reservas
 * pisa un bloqueo.
 */
public record ReservaDeUnRecurso(
        UUID id,
        String numero,
        String estado,
        OffsetDateTime desde,
        OffsetDateTime hasta) {
}
