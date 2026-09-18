package com.regenta.auditoria.aplicacion;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Una fila del watermark de cambios del servidor. HU-104. */
public record CambioDeServidor(long cursor, String entidadTipo, UUID entidadId, String operacion,
        String payload, OffsetDateTime ocurridoEn) {
}
