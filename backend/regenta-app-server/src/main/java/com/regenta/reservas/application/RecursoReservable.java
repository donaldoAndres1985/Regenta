package com.regenta.reservas.application;

import java.util.UUID;

/**
 * Un recurso del catÃ¡logo de servicio-recursos, con el buffer de su tipo (HU-069
 * criterio 4). El buffer va en minutos.
 */
public record RecursoReservable(
        UUID id,
        UUID tipoRecursoId,
        String codigo,
        String nombre,
        int capacidad,
        int bufferAntesMin,
        int bufferDespuesMin) {
}

