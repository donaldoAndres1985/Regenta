package com.regenta.inventario.domain;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Como se cuenta un producto: unidad, kilo, metro, litro. */
@Entity
@Table(name = "unidades_medida")
public class UnidadMedida {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(nullable = false, length = 10)
    private String codigo;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(name = "permite_decimales", nullable = false)
    private boolean permiteDecimales;

    @Column(name = "factor_base", nullable = false)
    private BigDecimal factorBase;

    @Column(nullable = false)
    private boolean activa;

    protected UnidadMedida() {
    }

    public static UnidadMedida nueva(UUID negocioId, String codigo, String nombre,
            boolean permiteDecimales) {
        UnidadMedida unidad = new UnidadMedida();
        unidad.id = UUID.randomUUID();
        unidad.negocioId = negocioId;
        unidad.codigo = codigo;
        unidad.nombre = nombre;
        unidad.permiteDecimales = permiteDecimales;
        unidad.factorBase = BigDecimal.ONE;
        unidad.activa = true;
        return unidad;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean isPermiteDecimales() {
        return permiteDecimales;
    }

    public boolean isActiva() {
        return activa;
    }
}
