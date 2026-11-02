package com.regenta.reservas.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Registrar el check-in de una reserva (HU-072). {@code recursoId} es opcional:
 * si la reserva se vendiÃ³ por tipo, se asigna aquÃ­; si se omite, el servicio
 * toma uno libre de ese tipo.
 */
public record SolicitudDeCheckIn(
        UUID recursoId,
        @PositiveOrZero BigDecimal deposito,
        @Size(max = 4000) String observacionesEntrada,
        @Valid List<SolicitudDeOcupante> ocupantes) {
}

