package com.regenta.menu.domain;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Una opción de un grupo (HU-078): "Término medio", "Extra queso". Puede llevar
 * un {@code precioExtra} que suma al total de la línea (criterio 3) y, si
 * descuenta insumo, un {@code productoId} + {@code cantidadInsumo} (para HU-079).
 */
@Entity
@Table(name = "modificadores")
public class Modificador {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "grupo_id", nullable = false, updatable = false)
    private UUID grupoId;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(name = "precio_extra", nullable = false)
    private BigDecimal precioExtra;

    @Column(name = "producto_id")
    private UUID productoId;

    @Column(name = "cantidad_insumo")
    private BigDecimal cantidadInsumo;

    @Column(nullable = false)
    private int orden;

    @Column(nullable = false)
    private boolean activo;

    protected Modificador() {
    }

    public static Modificador crear(UUID negocioId, UUID grupoId, String nombre,
            BigDecimal precioExtra, UUID productoId, BigDecimal cantidadInsumo, int orden) {
        Modificador m = new Modificador();
        m.id = UUID.randomUUID();
        m.negocioId = negocioId;
        m.grupoId = grupoId;
        m.activo = true;
        m.aplicar(nombre, precioExtra, productoId, cantidadInsumo, orden);
        return m;
    }

    public void editar(String nombre, BigDecimal precioExtra, UUID productoId,
            BigDecimal cantidadInsumo, int orden) {
        aplicar(nombre, precioExtra, productoId, cantidadInsumo, orden);
    }

    private void aplicar(String nombre, BigDecimal precioExtra, UUID productoId,
            BigDecimal cantidadInsumo, int orden) {
        this.nombre = nombre;
        this.precioExtra = precioExtra == null || precioExtra.signum() < 0 ? BigDecimal.ZERO
                : precioExtra;
        this.productoId = productoId;
        this.cantidadInsumo = cantidadInsumo == null || cantidadInsumo.signum() < 0 ? null
                : cantidadInsumo;
        this.orden = Math.max(0, orden);
    }

    public void activar() {
        this.activo = true;
    }

    public void desactivar() {
        this.activo = false;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getGrupoId() {
        return grupoId;
    }

    public String getNombre() {
        return nombre;
    }

    public BigDecimal getPrecioExtra() {
        return precioExtra;
    }

    public UUID getProductoId() {
        return productoId;
    }

    public BigDecimal getCantidadInsumo() {
        return cantidadInsumo;
    }

    public int getOrden() {
        return orden;
    }

    public boolean isActivo() {
        return activo;
    }
}
