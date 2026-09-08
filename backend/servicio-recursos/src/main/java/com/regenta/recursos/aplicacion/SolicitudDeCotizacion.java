package com.regenta.recursos.aplicacion;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Cotiza una estancia de un recurso: entrada, noches y personas (HU-066). */
public record SolicitudDeCotizacion(
        @NotNull UUID recursoId,
        @NotNull LocalDate fechaEntrada,
        @Positive int noches,
        @Positive int personas) {
}
