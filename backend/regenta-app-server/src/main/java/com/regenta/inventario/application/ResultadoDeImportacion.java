package com.regenta.inventario.application;

import java.util.List;

/** HU-164 C3/C5 · cuántos productos se crearon y las filas que se omitieron por error. */
public record ResultadoDeImportacion(int importados, List<FilaImportada> omitidas) {
}
