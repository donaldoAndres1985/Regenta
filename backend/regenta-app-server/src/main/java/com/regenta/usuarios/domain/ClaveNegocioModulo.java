package com.regenta.usuarios.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Clave de {@link NegocioModulo}: un modulo se activa una vez por negocio. */
@Embeddable
public class ClaveNegocioModulo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "negocio_id", nullable = false)
    private UUID negocioId;

    @Column(name = "modulo_codigo", nullable = false, length = 40)
    private String moduloCodigo;

    protected ClaveNegocioModulo() {
    }

    public ClaveNegocioModulo(UUID negocioId, String moduloCodigo) {
        this.negocioId = negocioId;
        this.moduloCodigo = moduloCodigo;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public String getModuloCodigo() {
        return moduloCodigo;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof ClaveNegocioModulo clave)) {
            return false;
        }
        return Objects.equals(negocioId, clave.negocioId)
                && Objects.equals(moduloCodigo, clave.moduloCodigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(negocioId, moduloCodigo);
    }
}
