package com.regenta.reportes.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** La dimensión sucursal. Se crea una vez por negocio+sucursal. */
@Entity
@Table(name = "dim_sucursal")
public class DimSucursal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sk;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "sucursal_id")
    private UUID sucursalId;

    @Column(name = "es_actual", nullable = false)
    private boolean esActual;

    protected DimSucursal() {
    }

    public static DimSucursal crear(UUID negocioId, UUID sucursalId) {
        DimSucursal d = new DimSucursal();
        d.negocioId = negocioId;
        d.sucursalId = sucursalId;
        d.esActual = true;
        return d;
    }

    public Long getSk() {
        return sk;
    }
}
