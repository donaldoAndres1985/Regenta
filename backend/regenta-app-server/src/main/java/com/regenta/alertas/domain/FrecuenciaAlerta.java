package com.regenta.alertas.domain;

/** Cada cuánto se evalúa/envía una regla (HU-092). Coincide con el CHECK de la tabla. */
public enum FrecuenciaAlerta {
    INMEDIATA,
    HORARIA,
    DIARIA,
    SEMANAL
}
