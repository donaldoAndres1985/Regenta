package com.regenta.mesas.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Clave compuesta de {@link MesaDeSesion}: {@code (sesion_id, mesa_id)}. */
public class MesaDeSesionId implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID sesionId;
    private UUID mesaId;

    public MesaDeSesionId() {
    }

    public MesaDeSesionId(UUID sesionId, UUID mesaId) {
        this.sesionId = sesionId;
        this.mesaId = mesaId;
    }

    public UUID getSesionId() {
        return sesionId;
    }

    public UUID getMesaId() {
        return mesaId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MesaDeSesionId otra)) {
            return false;
        }
        return Objects.equals(sesionId, otra.sesionId) && Objects.equals(mesaId, otra.mesaId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sesionId, mesaId);
    }
}
