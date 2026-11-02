package com.regenta.reservas.application;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * QuÃ© recursos estÃ¡n libres entre dos instantes (HU-069). {@code tipoRecursoId}
 * acota a un tipo; {@code personas} descarta los que no dan la capacidad.
 */
public record SolicitudDeDisponibilidad(
        @NotNull OffsetDateTime desde,
        @NotNull OffsetDateTime hasta,
        UUID tipoRecursoId,
        @Positive Integer personas) {
}

