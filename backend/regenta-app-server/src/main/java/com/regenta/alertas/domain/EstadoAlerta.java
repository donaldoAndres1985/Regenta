package com.regenta.alertas.domain;

/** El ciclo de una alerta (HU-092, HU-095). */
public enum EstadoAlerta {
    NUEVA,
    VISTA,
    EN_CURSO,
    RESUELTA,
    DESCARTADA;

    /** Todavía cuenta como pendiente: no se debe volver a generar la misma. */
    public boolean estaActiva() {
        return this == NUEVA || this == VISTA || this == EN_CURSO;
    }
}
