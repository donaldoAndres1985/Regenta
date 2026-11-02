package com.regenta.recursos.application;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Aplica una polÃ­tica a una reserva (HU-068 criterio 3). Sin {@code politicaId}
 * se usa la polÃ­tica por defecto del negocio.
 */
public record SolicitudDeAplicacionDeCancelacion(
        UUID politicaId,
        @NotNull @PositiveOrZero BigDecimal montoReserva,
        @NotNull OffsetDateTime entrada) {
}
