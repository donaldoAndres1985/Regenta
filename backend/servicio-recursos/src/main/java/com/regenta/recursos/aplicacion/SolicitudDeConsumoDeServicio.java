package com.regenta.recursos.aplicacion;

import java.util.UUID;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Agregar (cotizar) o consumir un servicio adicional en una reserva (HU-068).
 * Según el modo de cobro del servicio se usan {@code personas}/{@code noches} o
 * {@code cantidad}. Para consumir hay que decir en qué reserva.
 */
public record SolicitudDeConsumoDeServicio(
        UUID reservaId,
        @Positive int personas,
        @Positive int noches,
        @PositiveOrZero int cantidad) {
}
