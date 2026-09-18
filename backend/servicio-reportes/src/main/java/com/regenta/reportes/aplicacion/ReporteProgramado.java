package com.regenta.reportes.aplicacion;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Una programación tal como queda guardada. */
public record ReporteProgramado(
        UUID id,
        String codigo,
        String nombre,
        String cron,
        String formato,
        List<String> destinatarios,
        int dias,
        boolean activo,
        OffsetDateTime proximaEjecucion) {
}
