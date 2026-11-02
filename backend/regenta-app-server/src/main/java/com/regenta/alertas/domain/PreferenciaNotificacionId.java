package com.regenta.alertas.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Clave compuesta de {@link PreferenciaNotificacion}: (usuario, tipo). */
public class PreferenciaNotificacionId implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID usuarioId;
    private String tipoCodigo;

    public PreferenciaNotificacionId() {
    }

    public PreferenciaNotificacionId(UUID usuarioId, String tipoCodigo) {
        this.usuarioId = usuarioId;
        this.tipoCodigo = tipoCodigo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PreferenciaNotificacionId otra)) {
            return false;
        }
        return Objects.equals(usuarioId, otra.usuarioId)
                && Objects.equals(tipoCodigo, otra.tipoCodigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usuarioId, tipoCodigo);
    }
}
