package com.regenta.inventario.domain;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** HU-156 · una fracción de una tabla: medida, factor (lo que descuenta) y precio con IVA. */
@Entity
@Table(name = "fracciones", schema = "inventario")
public class Fraccion {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tabla_id", nullable = false, updatable = false)
    private TablaDeFracciones tabla;

    @Column(nullable = false, length = 30)
    private String medida;

    @Column(nullable = false)
    private BigDecimal factor;

    @Column(nullable = false)
    private BigDecimal precio;

    @Column(nullable = false)
    private short orden;

    protected Fraccion() {
    }

    static Fraccion de(TablaDeFracciones tabla, String medida, BigDecimal factor, BigDecimal precio,
            short orden) {
        Fraccion f = new Fraccion();
        f.id = UUID.randomUUID();
        f.negocioId = tabla.getNegocioId();
        f.tabla = tabla;
        f.medida = medida;
        f.factor = factor;
        f.precio = precio;
        f.orden = orden;
        return f;
    }

    void cambiar(BigDecimal factor, BigDecimal precio, short orden) {
        this.factor = factor;
        this.precio = precio;
        this.orden = orden;
    }

    public UUID getId() {
        return id;
    }

    public String getMedida() {
        return medida;
    }

    public BigDecimal getFactor() {
        return factor;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public short getOrden() {
        return orden;
    }
}
