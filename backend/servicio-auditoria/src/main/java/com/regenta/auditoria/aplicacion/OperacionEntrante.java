package com.regenta.auditoria.aplicacion;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/** Una operación tal como la manda el cliente dentro de un lote. HU-102. */
public record OperacionEntrante(UUID id, String idempotencyKey, long secuenciaLocal, String entidadTipo,
        UUID entidadId, String operacion, Map<String, Object> payload, Long versionBase,
        OffsetDateTime creadoClienteEn) {
}
