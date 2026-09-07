package com.regenta.inventario.domain;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * El precio de un producto en una lista, a partir de cierta cantidad. HU-036.
 *
 * <p>Varias filas por {@code (lista, producto)} con distinta
 * {@code cantidad_minima} arman los tramos de precio por volumen: de 1 a 11 un
 * precio, de 12 en adelante otro.
 */
@Entity
@Table(name = "precios_producto")
@IdClass(PrecioDeProductoId.class)
public class PrecioDeProducto {

    @Id
    @Column(name = "lista_id")
    private UUID listaId;

    @Id
    @Column(name = "producto_id")
    private UUID productoId;

    @Id
    @Column(name = "cantidad_minima")
    private BigDecimal cantidadMinima;

    @Column(name = "negocio_id", nullable = false)
    private UUID negocioId;

    @Column(nullable = false)
    private BigDecimal precio;

    @Column(name = "descuento_max_pct", nullable = false)
    private BigDecimal descuentoMaxPct;

    protected PrecioDeProducto() {
    }

    public static PrecioDeProducto de(UUID negocioId, UUID listaId, UUID productoId,
            BigDecimal cantidadMinima, BigDecimal precio, BigDecimal descuentoMaxPct) {
        PrecioDeProducto p = new PrecioDeProducto();
        p.negocioId = negocioId;
        p.listaId = listaId;
        p.productoId = productoId;
        p.cantidadMinima = cantidadMinima == null ? BigDecimal.ONE : cantidadMinima;
        p.precio = precio;
        p.descuentoMaxPct = descuentoMaxPct == null ? BigDecimal.ZERO : descuentoMaxPct;
        return p;
    }

    public void ajustar(BigDecimal precio, BigDecimal descuentoMaxPct) {
        if (precio != null) {
            this.precio = precio;
        }
        if (descuentoMaxPct != null) {
            this.descuentoMaxPct = descuentoMaxPct;
        }
    }

    public UUID getListaId() {
        return listaId;
    }

    public UUID getProductoId() {
        return productoId;
    }

    public BigDecimal getCantidadMinima() {
        return cantidadMinima;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public BigDecimal getDescuentoMaxPct() {
        return descuentoMaxPct;
    }
}
