package com.regenta.recursos.aplicacion;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Lo que servicio-recursos necesita saber de servicio-reservas: para no borrar
 * un recurso con reservas futuras (HU-065 criterio 3) y para advertir qué
 * reservas confirmadas pisa un bloqueo nuevo (HU-067 criterio 3). La
 * implementación real consulta por REST; hasta entonces, un stub la reemplaza.
 */
public interface ConsultaDeReservasDeRecurso {

    boolean tieneReservasFuturas(UUID negocioId, UUID recursoId);

    /** Reservas confirmadas del recurso que se solapan con {@code [desde, hasta)}. */
    List<ReservaAfectada> reservasConfirmadasEnPeriodo(UUID negocioId, UUID recursoId,
            OffsetDateTime desde, OffsetDateTime hasta);
}
