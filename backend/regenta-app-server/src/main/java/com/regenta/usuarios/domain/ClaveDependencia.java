package com.regenta.usuarios.domain;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Clave de {@link ModuloDependencia}. */
@Embeddable
public class ClaveDependencia implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "modulo_codigo", nullable = false, length = 40)
    private String moduloCodigo;

    @Column(name = "depende_de", nullable = false, length = 40)
    private String dependeDe;

    protected ClaveDependencia() {
    }

    public ClaveDependencia(String moduloCodigo, String dependeDe) {
        this.moduloCodigo = moduloCodigo;
        this.dependeDe = dependeDe;
    }

    public String getModuloCodigo() {
        return moduloCodigo;
    }

    public String getDependeDe() {
        return dependeDe;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof ClaveDependencia clave)) {
            return false;
        }
        return Objects.equals(moduloCodigo, clave.moduloCodigo)
                && Objects.equals(dependeDe, clave.dependeDe);
    }

    @Override
    public int hashCode() {
        return Objects.hash(moduloCodigo, dependeDe);
    }
}
