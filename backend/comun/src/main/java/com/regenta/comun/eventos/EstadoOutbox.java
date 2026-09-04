package com.regenta.comun.eventos;

/** Los tres estados que admite la columna, iguales al CHECK de la tabla. */
public enum EstadoOutbox {
    PENDIENTE,
    PUBLICADO,
    FALLIDO
}
