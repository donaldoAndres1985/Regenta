package com.regenta.inventario.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** PK compuesta de {@code existencias}: {@code (producto_id, bodega_id)}. */
public class ExistenciaId implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID productoId;
    private UUID bodegaId;

    public ExistenciaId() {
    }

    public ExistenciaId(UUID productoId, UUID bodegaId) {
        this.productoId = productoId;
        this.bodegaId = bodegaId;
    }

    public UUID getProductoId() {
        return productoId;
    }

    public UUID getBodegaId() {
        return bodegaId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExistenciaId otra)) {
            return false;
        }
        return Objects.equals(productoId, otra.productoId)
                && Objects.equals(bodegaId, otra.bodegaId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productoId, bodegaId);
    }
}
