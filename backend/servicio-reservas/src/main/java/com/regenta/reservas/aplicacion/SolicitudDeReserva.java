package com.regenta.reservas.aplicacion;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Crear una reserva de un recurso para un periodo (HU-070). El periodo llega
 * como dos instantes ya construidos por el cliente con las horas de check-in y
 * check-out del negocio.
 */
public record SolicitudDeReserva(
        @NotNull OffsetDateTime desde,
        @NotNull OffsetDateTime hasta,
        @NotNull UUID tipoRecursoId,
        @NotNull UUID recursoId,
        UUID clienteId,
        UUID sucursalId,
        @PositiveOrZero Integer numAdultos,
        @PositiveOrZero Integer numNinos,
        UUID politicaCancelacionId,
        String canal,
        @Size(max = 4000) String notas) {
}
