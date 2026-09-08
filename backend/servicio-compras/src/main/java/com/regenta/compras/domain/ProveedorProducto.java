package com.regenta.compras.domain;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Qué producto vende un proveedor, con su código y su último costo (HU-046). Es
 * lo que evita retipear costos al crear una orden (criterio 2) y lo que marca al
 * proveedor preferido de un producto para la sugerencia de compra (criterio 3).
 */
@Entity
@Table(name = "proveedor_productos")
@IdClass(ProveedorProductoId.class)
public class ProveedorProducto {

    @Id
    @Column(name = "proveedor_id")
    private UUID proveedorId;

    @Id
    @Column(name = "producto_id")
    private UUID productoId;

    @Column(name = "negocio_id", nullable = false)
    private UUID negocioId;

    @Column(name = "codigo_proveedor", length = 60)
    private String codigoProveedor;

    @Column(name = "costo_ultimo")
    private BigDecimal costoUltimo;

    @Column(name = "dias_entrega")
    private Short diasEntrega;

    @Column(name = "cantidad_minima", nullable = false)
    private BigDecimal cantidadMinima;

    @Column(nullable = false)
    private boolean preferido;

    protected ProveedorProducto() {
    }

    public static ProveedorProducto de(UUID negocioId, UUID proveedorId, UUID productoId,
            String codigoProveedor, BigDecimal costoUltimo, Short diasEntrega,
            BigDecimal cantidadMinima, boolean preferido) {
        ProveedorProducto pp = new ProveedorProducto();
        pp.negocioId = negocioId;
        pp.proveedorId = proveedorId;
        pp.productoId = productoId;
        pp.codigoProveedor = codigoProveedor;
        pp.costoUltimo = costoUltimo;
        pp.diasEntrega = diasEntrega;
        pp.cantidadMinima = cantidadMinima == null || cantidadMinima.signum() <= 0
                ? BigDecimal.ONE : cantidadMinima;
        pp.preferido = preferido;
        return pp;
    }

    public void actualizar(String codigoProveedor, BigDecimal costoUltimo, Short diasEntrega,
            BigDecimal cantidadMinima, boolean preferido) {
        this.codigoProveedor = codigoProveedor;
        this.costoUltimo = costoUltimo;
        this.diasEntrega = diasEntrega;
        this.cantidadMinima = cantidadMinima == null || cantidadMinima.signum() <= 0
                ? BigDecimal.ONE : cantidadMinima;
        this.preferido = preferido;
    }

    public UUID getProveedorId() {
        return proveedorId;
    }

    public UUID getProductoId() {
        return productoId;
    }

    public String getCodigoProveedor() {
        return codigoProveedor;
    }

    public BigDecimal getCostoUltimo() {
        return costoUltimo;
    }

    public Short getDiasEntrega() {
        return diasEntrega;
    }

    public BigDecimal getCantidadMinima() {
        return cantidadMinima;
    }

    public boolean isPreferido() {
        return preferido;
    }
}
