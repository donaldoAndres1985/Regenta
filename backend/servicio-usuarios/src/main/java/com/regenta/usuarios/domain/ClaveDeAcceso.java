package com.regenta.usuarios.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Clave de {@link AccesoPorCorreo}: un correo puede estar en varios negocios. */
@Embeddable
public class ClaveDeAcceso implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(name = "negocio_id", nullable = false)
    private UUID negocioId;

    protected ClaveDeAcceso() {
    }

    public ClaveDeAcceso(String email, UUID negocioId) {
        this.email = email;
        this.negocioId = negocioId;
    }

    public String getEmail() {
        return email;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof ClaveDeAcceso clave)) {
            return false;
        }
        return Objects.equals(email, clave.email) && Objects.equals(negocioId, clave.negocioId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, negocioId);
    }
}
