package com.regenta.recursos.aplicacion;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Si un recurso se puede reservar entre dos instantes (HU-067 criterio 1).
 * {@code disponible} es false en cuanto hay un bloqueo que pisa el periodo;
 * {@code bloqueos} son esos bloqueos.
 */
public record DisponibilidadDeRecurso(
        UUID recursoId,
        OffsetDateTime desde,
        OffsetDateTime hasta,
        boolean disponible,
        List<BloqueoDelNegocio> bloqueos) {
}
