package com.regenta.comandas.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Clave compuesta de {@link CuentaLinea}: {@code (cuenta_id, comanda_linea_id)}. */
public class CuentaLineaId implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID cuentaId;
    private UUID comandaLineaId;

    public CuentaLineaId() {
    }

    public CuentaLineaId(UUID cuentaId, UUID comandaLineaId) {
        this.cuentaId = cuentaId;
        this.comandaLineaId = comandaLineaId;
    }

    public UUID getCuentaId() {
        return cuentaId;
    }

    public UUID getComandaLineaId() {
        return comandaLineaId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CuentaLineaId otra)) {
            return false;
        }
        return Objects.equals(cuentaId, otra.cuentaId) && Objects.equals(comandaLineaId, otra.comandaLineaId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cuentaId, comandaLineaId);
    }
}
