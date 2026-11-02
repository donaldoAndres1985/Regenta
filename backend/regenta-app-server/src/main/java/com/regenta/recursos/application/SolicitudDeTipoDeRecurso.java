package com.regenta.recursos.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Alta o ediciÃ³n de un tipo de recurso (HU-064). */
public record SolicitudDeTipoDeRecurso(
        @NotBlank @Size(max = 100) String nombre,
        String descripcion,
        String unidadTiempo,
        @PositiveOrZero Integer duracionMinimaMin,
        @PositiveOrZero Integer incrementoMin,
        @PositiveOrZero Integer capacidadDefault,
        Boolean permiteOverbooking,
        @PositiveOrZero Integer bufferAntesMin,
        @PositiveOrZero Integer bufferDespuesMin) {
}
