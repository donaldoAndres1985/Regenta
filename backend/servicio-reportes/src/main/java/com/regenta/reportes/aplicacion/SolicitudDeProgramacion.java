package com.regenta.reportes.aplicacion;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

/**
 * Programar un reporte (HU-100 criterio 2).
 *
 * @param dias cuántos días hacia atrás cubre cada ejecución; un semanal
 *             normalmente quiere 7. Sin esto habría que elegir el rango por
 *             el cron, y el cron dice cuándo, no qué.
 */
public record SolicitudDeProgramacion(
        @NotBlank String codigo,
        @NotBlank String nombre,
        @NotBlank String cron,
        @NotBlank String formato,
        List<String> destinatarios,
        Integer dias) {
}
