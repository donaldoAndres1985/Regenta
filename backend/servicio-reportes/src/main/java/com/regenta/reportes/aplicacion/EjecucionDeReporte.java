package com.regenta.reportes.aplicacion;

import java.time.OffsetDateTime;
import java.util.UUID;

/** El comprobante de una exportación: lo que se devuelve al pedirla y al consultarla. */
public record EjecucionDeReporte(
        UUID id,
        String codigo,
        String formato,
        String estado,
        Integer filas,
        String archivoNombre,
        String error,
        int intentos,
        OffsetDateTime iniciadoEn,
        OffsetDateTime finalizadoEn) {
}
