package com.regenta.recursos.aplicacion;

import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Alta o edición de un recurso individual (HU-065). */
public record SolicitudDeRecurso(
        @NotNull UUID tipoRecursoId,
        @NotBlank @Size(max = 30) String codigo,
        @NotBlank @Size(max = 120) String nombre,
        String descripcion,
        UUID sucursalId,
        @PositiveOrZero Integer capacidad,
        String piso,
        String zona,
        Map<String, Object> atributos,
        String imagenUrl) {
}
