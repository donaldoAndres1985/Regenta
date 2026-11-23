package com.regenta.reportes.application;

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
        OffsetDateTime finalizadoEn,
        /** HU-121 C5: se generó bien y su archivo ya se borró por retención (no es un fallo). */
        boolean archivoExpirado) {
}
