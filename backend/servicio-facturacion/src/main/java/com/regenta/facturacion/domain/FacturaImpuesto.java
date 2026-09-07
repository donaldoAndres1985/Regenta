package com.regenta.facturacion.domain;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Un impuesto (o retención) de una factura, ligado o no a una línea. */
@Entity
@Table(name = "factura_impuestos")
public class FacturaImpuesto {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "factura_id", nullable = false, updatable = false)
    private UUID facturaId;

    @Column(name = "factura_linea_id")
    private UUID facturaLineaId;

    @Column(nullable = false, length = 10)
    private String codigo;

    @Column(nullable = false, length = 40)
    private String nombre;

    @Column(nullable = false)
    private BigDecimal porcentaje;

    @Column(nullable = false)
    private BigDecimal base;

    @Column(nullable = false)
    private BigDecimal valor;

    @Column(name = "es_retencion", nullable = false)
    private boolean esRetencion;

    protected FacturaImpuesto() {
    }

    static FacturaImpuesto de(UUID negocioId, UUID facturaId, UUID facturaLineaId, String codigo,
            String nombre, BigDecimal porcentaje, BigDecimal base, BigDecimal valor,
            boolean esRetencion) {
        FacturaImpuesto i = new FacturaImpuesto();
        i.id = UUID.randomUUID();
        i.negocioId = negocioId;
        i.facturaId = facturaId;
        i.facturaLineaId = facturaLineaId;
        i.codigo = codigo;
        i.nombre = nombre;
        i.porcentaje = porcentaje;
        i.base = base;
        i.valor = valor;
        i.esRetencion = esRetencion;
        return i;
    }

    public UUID getId() {
        return id;
    }

    public UUID getFacturaLineaId() {
        return facturaLineaId;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public BigDecimal getPorcentaje() {
        return porcentaje;
    }

    public BigDecimal getBase() {
        return base;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public boolean isEsRetencion() {
        return esRetencion;
    }
}
