package com.regenta.recursos.domain;

/**
 * En qué estado operativo está un recurso (HU-065). Solo {@link #DISPONIBLE}
 * aparece como reservable; el CHECK de {@code recursos.estado} lleva esta lista.
 */
public enum EstadoRecurso {
    DISPONIBLE,
    OCUPADO,
    LIMPIEZA,
    MANTENIMIENTO,
    FUERA_SERVICIO;

    public boolean disponibleParaReservar() {
        return this == DISPONIBLE;
    }
}
