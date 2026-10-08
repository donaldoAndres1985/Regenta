package com.regenta.auditoria.aplicacion;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Una fila de la bitácora, tal como la consulta HU-101/HU-105.
 *
 * @param cambios HU-130: los campos que cambiaron, {@code {"precio": {"antes": 100,
 *                "despues": 120}}}; {@code null} si el servicio que escribió no
 *                está instrumentado.
 */
public record EventoDeAuditoria(UUID id, UUID usuarioId, String servicio, String entidadTipo,
        UUID entidadId, String accion, String traceId, String resultado, OffsetDateTime ocurridoEn,
        Map<String, Object> cambios) {
}
