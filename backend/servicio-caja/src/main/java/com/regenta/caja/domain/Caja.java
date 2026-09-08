package com.regenta.caja.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Un punto de cobro del negocio (HU-059). Las sesiones se abren contra una caja. */
@Entity
@Table(name = "cajas")
public class Caja {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "sucursal_id")
    private UUID sucursalId;

    @Column(nullable = false, length = 20)
    private String codigo;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(name = "terminal_id", length = 80)
    private String terminalId;

    @Column(nullable = false)
    private boolean activa;

    protected Caja() {
    }

    public static Caja crear(UUID negocioId, UUID sucursalId, String codigo, String nombre,
            String terminalId) {
        Caja c = new Caja();
        c.id = UUID.randomUUID();
        c.negocioId = negocioId;
        c.sucursalId = sucursalId;
        c.codigo = codigo;
        c.nombre = nombre;
        c.terminalId = terminalId;
        c.activa = true;
        return c;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getSucursalId() {
        return sucursalId;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean isActiva() {
        return activa;
    }
}
