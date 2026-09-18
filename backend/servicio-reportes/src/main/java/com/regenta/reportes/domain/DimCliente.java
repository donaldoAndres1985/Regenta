package com.regenta.reportes.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** La dimensión cliente. Se crea una vez por negocio+cliente; no lleva SCD2 propio en HU-096. */
@Entity
@Table(name = "dim_cliente")
public class DimCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sk;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "cliente_id", nullable = false, updatable = false)
    private UUID clienteId;

    @Column(length = 200)
    private String nombre;

    @Column(name = "es_actual", nullable = false)
    private boolean esActual;

    protected DimCliente() {
    }

    public static DimCliente crear(UUID negocioId, UUID clienteId, String nombre) {
        DimCliente d = new DimCliente();
        d.negocioId = negocioId;
        d.clienteId = clienteId;
        d.nombre = nombre;
        d.esActual = true;
        return d;
    }

    public Long getSk() {
        return sk;
    }
}
