package com.regenta.inventario.application;

import java.util.List;

/** HU-164 C2 · cada fila del archivo con sus errores, antes de grabar. */
public record VistaPreviaDeImportacion(List<FilaImportada> filas, int validas, int conError) {
}
