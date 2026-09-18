package com.regenta.auditoria.aplicacion;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Una fila de la bitácora, tal como la consulta HU-101/HU-105. */
public record EventoDeAuditoria(UUID id, UUID usuarioId, String servicio, String entidadTipo,
        UUID entidadId, String accion, String traceId, String resultado, OffsetDateTime ocurridoEn) {
}
