package com.regenta.ventas.domain;

/** El CHECK de {@code sagas.estado} lleva esta lista. */
public enum EstadoSaga {
    INICIADA,
    ESPERANDO_STOCK,
    COMPLETADA,
    COMPENSANDO,
    COMPENSADA,
    FALLIDA
}
