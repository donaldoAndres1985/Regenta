package com.regenta.auditoria.aplicacion;

import java.util.UUID;

import com.regenta.auditoria.domain.EstadoOperacion;

/** Lo que el servidor responde por cada operación del lote. HU-102. */
public record ResultadoDeOperacion(UUID id, EstadoOperacion estado, String error) {
}
