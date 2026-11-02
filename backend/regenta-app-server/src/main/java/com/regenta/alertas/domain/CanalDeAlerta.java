package com.regenta.alertas.domain;

/** Por dónde sale una alerta. La entrega real (push, correo) es HU-094. */
public enum CanalDeAlerta {
    IN_APP,
    PUSH,
    EMAIL,
    SMS,
    WEBHOOK
}
