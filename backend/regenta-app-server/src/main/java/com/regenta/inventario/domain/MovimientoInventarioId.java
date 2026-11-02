package com.regenta.inventario.domain;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/** PK de {@code movimientos_inventario}: incluye la columna de particion. */
public class MovimientoInventarioId implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;
    private OffsetDateTime ocurridoEn;

    public MovimientoInventarioId() {
    }

    public MovimientoInventarioId(UUID id, OffsetDateTime ocurridoEn) {
        this.id = id;
        this.ocurridoEn = ocurridoEn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MovimientoInventarioId otro)) {
            return false;
        }
        return Objects.equals(id, otro.id) && Objects.equals(ocurridoEn, otro.ocurridoEn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ocurridoEn);
    }
}
