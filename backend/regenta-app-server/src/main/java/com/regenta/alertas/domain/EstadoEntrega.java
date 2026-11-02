package com.regenta.alertas.domain;

/** El ciclo de una entrega de alerta (HU-094). Coincide con el CHECK de la tabla. */
public enum EstadoEntrega {
    PENDIENTE,
    ENVIADA,
    ENTREGADA,
    FALLIDA,
    LEIDA
}
