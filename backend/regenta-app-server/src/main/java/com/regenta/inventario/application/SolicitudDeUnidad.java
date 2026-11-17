package com.regenta.inventario.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** HU-155 · alta o edición de una unidad de medida. El código se guarda en mayúsculas. */
public record SolicitudDeUnidad(
        @NotBlank @Size(max = 10) String codigo,
        @NotBlank @Size(max = 50) String nombre,
        boolean permiteDecimales) {
}
