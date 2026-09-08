package com.regenta.recursos.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Alta o edición de una tarifa (HU-066). El destino es un tipo o un recurso. */
public record SolicitudDeTarifa(
        UUID tipoRecursoId,
        UUID recursoId,
        @NotBlank @Size(max = 80) String nombre,
        String unidadTiempo,
        @NotNull @PositiveOrZero BigDecimal precioBase,
        @PositiveOrZero BigDecimal precioPersonaAdicional,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta,
        List<Integer> diasSemana,
        LocalTime horaDesde,
        LocalTime horaHasta,
        @PositiveOrZero Integer estanciaMinima,
        Integer prioridad) {
}
