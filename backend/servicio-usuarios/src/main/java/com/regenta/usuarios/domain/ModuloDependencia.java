package com.regenta.usuarios.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * El grafo del catalogo: que modulo necesita a cual. Es lo que impide activar
 * FACTURACION sin VENTAS, o MESAS sin COMANDAS.
 */
@Entity
@Table(name = "modulo_dependencias")
public class ModuloDependencia {

    @EmbeddedId
    private ClaveDependencia clave;

    @Column(nullable = false)
    private boolean obligatoria;

    protected ModuloDependencia() {
    }

    public String getModuloCodigo() {
        return clave.getModuloCodigo();
    }

    public String getDependeDe() {
        return clave.getDependeDe();
    }

    public boolean isObligatoria() {
        return obligatoria;
    }
}
