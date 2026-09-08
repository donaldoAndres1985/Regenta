package com.regenta.recursos.aplicacion;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Una reserva confirmada que cae dentro de un bloqueo que se acaba de crear
 * (HU-067 criterio 3). El dato lo trae servicio-reservas; aquí solo se muestra
 * como advertencia, el bloqueo se crea igual.
 */
public record ReservaAfectada(
        UUID reservaId,
        String codigo,
        OffsetDateTime entrada,
        OffsetDateTime salida,
        String huesped) {
}
