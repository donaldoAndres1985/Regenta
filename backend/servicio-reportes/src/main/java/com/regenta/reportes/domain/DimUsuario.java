package com.regenta.reportes.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** La dimensión usuario (quién vendió, quién fue el mesero). Se crea una vez por negocio+usuario. */
@Entity
@Table(name = "dim_usuario")
public class DimUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sk;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @Column(name = "es_actual", nullable = false)
    private boolean esActual;

    protected DimUsuario() {
    }

    public static DimUsuario crear(UUID negocioId, UUID usuarioId) {
        DimUsuario d = new DimUsuario();
        d.negocioId = negocioId;
        d.usuarioId = usuarioId;
        d.esActual = true;
        return d;
    }

    public Long getSk() {
        return sk;
    }
}
