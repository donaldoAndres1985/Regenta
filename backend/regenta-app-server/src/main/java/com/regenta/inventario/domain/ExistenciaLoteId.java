package com.regenta.inventario.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** PK compuesta de {@code existencias_lote}: {@code (lote_id, bodega_id)}. */
public class ExistenciaLoteId implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID loteId;
    private UUID bodegaId;

    public ExistenciaLoteId() {
    }

    public ExistenciaLoteId(UUID loteId, UUID bodegaId) {
        this.loteId = loteId;
        this.bodegaId = bodegaId;
    }

    public UUID getLoteId() {
        return loteId;
    }

    public UUID getBodegaId() {
        return bodegaId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExistenciaLoteId otra)) {
            return false;
        }
        return Objects.equals(loteId, otra.loteId) && Objects.equals(bodegaId, otra.bodegaId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(loteId, bodegaId);
    }
}
