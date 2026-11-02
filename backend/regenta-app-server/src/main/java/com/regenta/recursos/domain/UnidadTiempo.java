package com.regenta.recursos.domain;

/**
 * En qué unidad se reserva un tipo de recurso (HU-064 criterio 2). El CHECK de
 * {@code tipos_recurso.unidad_tiempo} lleva exactamente esta lista.
 */
public enum UnidadTiempo {
    MINUTO,
    HORA,
    NOCHE,
    DIA,
    SESION
}
