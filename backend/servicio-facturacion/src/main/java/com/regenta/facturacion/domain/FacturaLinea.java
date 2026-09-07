package com.regenta.facturacion.domain;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Una línea de una factura. Sus números vienen del documento de origen. */
@Entity
@Table(name = "factura_lineas")
public class FacturaLinea {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "factura_id", nullable = false, updatable = false)
    private UUID facturaId;

    @Column(nullable = false)
    private short linea;

    @Column(length = 60)
    private String codigo;

    @Column(nullable = false, length = 300)
    private String descripcion;

    @Column(nullable = false)
    private BigDecimal cantidad;

    @Column(name = "unidad_codigo", nullable = false, length = 10)
    private String unidadCodigo;

    @Column(name = "precio_unitario", nullable = false)
    private BigDecimal precioUnitario;

    @Column(name = "descuento_pct", nullable = false)
    private BigDecimal descuentoPct;

    @Column(name = "descuento_valor", nullable = false)
    private BigDecimal descuentoValor;

    @Column(name = "base_gravable", nullable = false)
    private BigDecimal baseGravable;

    @Column(nullable = false)
    private BigDecimal total;

    protected FacturaLinea() {
    }

    static FacturaLinea de(UUID negocioId, UUID facturaId, short linea, String codigo,
            String descripcion, BigDecimal cantidad, String unidadCodigo, BigDecimal precioUnitario,
            BigDecimal descuentoPct, BigDecimal descuentoValor, BigDecimal baseGravable,
            BigDecimal total) {
        FacturaLinea l = new FacturaLinea();
        l.id = UUID.randomUUID();
        l.negocioId = negocioId;
        l.facturaId = facturaId;
        l.linea = linea;
        l.codigo = codigo;
        l.descripcion = descripcion;
        l.cantidad = cantidad;
        l.unidadCodigo = unidadCodigo == null || unidadCodigo.isBlank() ? "94" : unidadCodigo;
        l.precioUnitario = precioUnitario;
        l.descuentoPct = descuentoPct == null ? BigDecimal.ZERO : descuentoPct;
        l.descuentoValor = descuentoValor == null ? BigDecimal.ZERO : descuentoValor;
        l.baseGravable = baseGravable;
        l.total = total;
        return l;
    }

    void fijarTotal(BigDecimal total) {
        this.total = total;
    }

    public UUID getId() {
        return id;
    }

    public short getLinea() {
        return linea;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public String getUnidadCodigo() {
        return unidadCodigo;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public BigDecimal getDescuentoPct() {
        return descuentoPct;
    }

    public BigDecimal getDescuentoValor() {
        return descuentoValor;
    }

    public BigDecimal getBaseGravable() {
        return baseGravable;
    }

    public BigDecimal getTotal() {
        return total;
    }
}
