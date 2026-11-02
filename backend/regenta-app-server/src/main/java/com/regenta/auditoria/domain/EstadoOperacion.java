package com.regenta.auditoria.domain;

/** Los valores que acepta el CHECK de {@code operaciones_sync.estado}. */
public enum EstadoOperacion {
    RECIBIDA,
    APLICADA,
    CONFLICTO,
    RECHAZADA,
    DESCARTADA
}
