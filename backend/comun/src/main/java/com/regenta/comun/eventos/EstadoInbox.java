package com.regenta.comun.eventos;

/** Los cuatro estados que admite la columna, iguales al CHECK de la tabla. */
public enum EstadoInbox {
    RECIBIDO,
    PROCESADO,
    DESCARTADO,
    ERROR
}
