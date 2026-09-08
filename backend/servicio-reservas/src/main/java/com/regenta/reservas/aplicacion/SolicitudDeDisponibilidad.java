package com.regenta.reservas.aplicacion;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Qué recursos están libres entre dos instantes (HU-069). {@code tipoRecursoId}
 * acota a un tipo; {@code personas} descarta los que no dan la capacidad.
 */
public record SolicitudDeDisponibilidad(
        @NotNull OffsetDateTime desde,
        @NotNull OffsetDateTime hasta,
        UUID tipoRecursoId,
        @Positive Integer personas) {
}
