package com.regenta.comandas.domain;

import java.util.List;

import com.regenta.comun.errores.ReglaDeNegocioException;

/**
 * El ciclo de vida del ticket de cocina (HU-088): agrupa las líneas de una
 * misma estación enviadas juntas y avanza aparte del ciclo de cada línea.
 */
public enum EstadoDeTicket {
    NUEVO, EN_PREPARACION, LISTO, ENTREGADO, ANULADO;

    private static final List<EstadoDeTicket> AVANCE =
            List.of(NUEVO, EN_PREPARACION, LISTO, ENTREGADO);

    /** El siguiente estado en el avance normal. Lanza si ya no hay siguiente. */
    public EstadoDeTicket siguiente() {
        int i = AVANCE.indexOf(this);
        if (i < 0 || i + 1 >= AVANCE.size()) {
            throw new ReglaDeNegocioException("El ticket ya está en " + this + ": no avanza más");
        }
        return AVANCE.get(i + 1);
    }

    /** Los estados que se ven en las columnas del KDS (HU-088 criterio 2). */
    public boolean visibleEnKds() {
        return this == NUEVO || this == EN_PREPARACION || this == LISTO;
    }
}
