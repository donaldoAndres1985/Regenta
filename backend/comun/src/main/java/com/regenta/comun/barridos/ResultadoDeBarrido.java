package com.regenta.comun.barridos;

import java.util.Map;
import java.util.UUID;

/**
 * Lo que hizo una pasada de un barrido, por negocio (HU-124 criterio 4).
 *
 * @param tomadasPorNegocio cuántas filas tomó cada negocio que terminó bien
 * @param fallos            el error de cada negocio cuyo trabajo falló
 */
public record ResultadoDeBarrido(String barrido, Map<UUID, Integer> tomadasPorNegocio,
        Map<UUID, String> fallos) {

    public int tomadas() {
        return tomadasPorNegocio.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int fallidos() {
        return fallos.size();
    }
}
