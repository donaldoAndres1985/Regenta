package com.regenta.reportes.aplicacion;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;

/** Lo que se pide para exportar un reporte (HU-100). */
public record SolicitudDeExportacion(
        @NotBlank String codigo,
        @NotBlank String formato,
        LocalDate desde,
        LocalDate hasta) {
}
