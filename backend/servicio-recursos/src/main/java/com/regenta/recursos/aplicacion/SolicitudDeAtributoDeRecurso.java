package com.regenta.recursos.aplicacion;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Un campo configurable de un tipo de recurso (HU-064 criterio 1). */
public record SolicitudDeAtributoDeRecurso(
        @NotBlank @Size(max = 50) String nombreCampo,
        @NotBlank @Size(max = 80) String etiqueta,
        String tipo,
        boolean obligatorio,
        List<String> opciones,
        @PositiveOrZero Integer orden) {
}
