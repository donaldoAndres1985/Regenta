package com.regenta.recursos.aplicacion;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Aplica una política a una reserva (HU-068 criterio 3). Sin {@code politicaId}
 * se usa la política por defecto del negocio.
 */
public record SolicitudDeAplicacionDeCancelacion(
        UUID politicaId,
        @NotNull @PositiveOrZero BigDecimal montoReserva,
        @NotNull OffsetDateTime entrada) {
}
