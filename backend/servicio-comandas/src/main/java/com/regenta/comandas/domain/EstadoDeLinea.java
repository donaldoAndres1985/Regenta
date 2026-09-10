package com.regenta.comandas.domain;

import java.util.List;

import com.regenta.comun.errores.ReglaDeNegocioException;

/**
 * El ciclo de vida propio de cada línea (HU-086): recorre PENDIENTE, ENVIADA,
 * EN_PREPARACION, LISTA, ENTREGADA. Esto es lo que no existe en una venta.
 */
public enum EstadoDeLinea {
    PENDIENTE, ENVIADA, EN_PREPARACION, LISTA, ENTREGADA, ANULADA;

    private static final List<EstadoDeLinea> AVANCE =
            List.of(PENDIENTE, ENVIADA, EN_PREPARACION, LISTA, ENTREGADA);

    /** El siguiente estado en el avance normal. Lanza si ya no hay siguiente. */
    public EstadoDeLinea siguiente() {
        int i = AVANCE.indexOf(this);
        if (i < 0 || i + 1 >= AVANCE.size()) {
            throw new ReglaDeNegocioException("La línea ya está en " + this + ": no avanza más");
        }
        return AVANCE.get(i + 1);
    }

    public boolean yaEnviada() {
        return this == ENVIADA || this == EN_PREPARACION || this == LISTA || this == ENTREGADA;
    }
}
