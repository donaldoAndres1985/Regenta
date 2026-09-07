package com.regenta.inventario.aplicacion;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Alta de una lista de precios. Las fechas de vigencia son opcionales. */
public record SolicitudDeLista(
        @NotBlank @Size(max = 80) String nombre,
        @Size(min = 3, max = 3) String moneda,
        boolean esDefault,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta) {
}
