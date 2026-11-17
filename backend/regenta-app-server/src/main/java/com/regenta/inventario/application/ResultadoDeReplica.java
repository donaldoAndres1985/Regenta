package com.regenta.inventario.application;

import java.util.List;

/** HU-161 C3 · cuántas equivalencias se crearon y qué productos se quedaron sin par. */
public record ResultadoDeReplica(int creadas, List<String> sinPar) {
}
