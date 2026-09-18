package com.regenta.auditoria.aplicacion;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Un conflicto con lo del servidor y lo del cliente lado a lado. HU-103 criterio 2. */
public record ConflictoDetalle(UUID id, String entidadTipo, UUID entidadId, String tipo,
        String datosServidor, String datosCliente, String resolucion, OffsetDateTime detectadoEn) {
}
