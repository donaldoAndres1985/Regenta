package com.regenta.menu.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** Clave compuesta de {@link DisponibilidadDiaria}: {@code (item_id, fecha)}. */
public class DisponibilidadDiariaId implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID itemId;
    private LocalDate fecha;

    public DisponibilidadDiariaId() {
    }

    public DisponibilidadDiariaId(UUID itemId, LocalDate fecha) {
        this.itemId = itemId;
        this.fecha = fecha;
    }

    public UUID getItemId() {
        return itemId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DisponibilidadDiariaId otra)) {
            return false;
        }
        return Objects.equals(itemId, otra.itemId) && Objects.equals(fecha, otra.fecha);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId, fecha);
    }
}
