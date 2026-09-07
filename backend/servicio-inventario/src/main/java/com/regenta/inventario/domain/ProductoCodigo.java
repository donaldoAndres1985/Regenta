package com.regenta.inventario.domain;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Un código de barras alterno de un producto. HU-035.
 *
 * <p>Sirve para el empaque: la caja de 12 tiene su propio código y un
 * {@code factor} de conversión (1 caja = 12 unidades). El código de barras
 * principal vive en {@code productos.codigo_barras} y siempre tiene factor 1.
 */
@Entity
@Table(name = "producto_codigos")
public class ProductoCodigo {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "producto_id", nullable = false, updatable = false)
    private UUID productoId;

    @Column(nullable = false, length = 60, updatable = false)
    private String codigo;

    @Column(nullable = false)
    private BigDecimal factor;

    @Column(length = 60)
    private String descripcion;

    protected ProductoCodigo() {
    }

    public static ProductoCodigo de(UUID negocioId, UUID productoId, String codigo,
            BigDecimal factor, String descripcion) {
        ProductoCodigo c = new ProductoCodigo();
        c.id = UUID.randomUUID();
        c.negocioId = negocioId;
        c.productoId = productoId;
        c.codigo = codigo;
        c.factor = factor == null ? BigDecimal.ONE : factor;
        c.descripcion = descripcion;
        return c;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductoId() {
        return productoId;
    }

    public String getCodigo() {
        return codigo;
    }

    public BigDecimal getFactor() {
        return factor;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
