package com.regenta.usuarios.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Catalogo global de permisos: MODULO_RECURSO_ACCION. */
@Entity
@Table(name = "permisos")
public class Permiso {

    @Id
    @Column(length = 80)
    private String codigo;

    @Column(name = "modulo_codigo", nullable = false, length = 40)
    private String moduloCodigo;

    @Column(nullable = false, length = 40)
    private String recurso;

    @Column(nullable = false, length = 20)
    private String accion;

    protected Permiso() {
    }

    public String getCodigo() {
        return codigo;
    }

    public String getModuloCodigo() {
        return moduloCodigo;
    }

    public String getRecurso() {
        return recurso;
    }

    public String getAccion() {
        return accion;
    }
}
