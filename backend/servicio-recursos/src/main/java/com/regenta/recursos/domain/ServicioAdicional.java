package com.regenta.recursos.domain;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Un extra que el negocio ofrece sobre una reserva (HU-068): desayuno,
 * parqueadero, alquiler de equipo. El {@code codigo} es único por negocio
 * ({@code uq_servicio_adicional}). El {@link ModoCobro} decide cuántas unidades
 * se cobran; si tiene {@code productoId}, consumirlo descuenta inventario.
 */
@Entity
@Table(name = "servicios_adicionales")
public class ServicioAdicional {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(nullable = false, length = 30, updatable = false)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(columnDefinition = "text")
    private String descripcion;

    @Column(nullable = false)
    private BigDecimal precio;

    @Column(name = "impuesto_id")
    private UUID impuestoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo_cobro", nullable = false, length = 20)
    private ModoCobro modoCobro;

    @Column(name = "producto_id")
    private UUID productoId;

    @Column(nullable = false)
    private boolean activo;

    protected ServicioAdicional() {
    }

    public static ServicioAdicional crear(UUID negocioId, String codigo, String nombre,
            String descripcion, BigDecimal precio, UUID impuestoId, ModoCobro modoCobro,
            UUID productoId) {
        ServicioAdicional s = new ServicioAdicional();
        s.id = UUID.randomUUID();
        s.negocioId = negocioId;
        s.codigo = codigo;
        s.activo = true;
        s.aplicar(nombre, descripcion, precio, impuestoId, modoCobro, productoId);
        return s;
    }

    public void editar(String nombre, String descripcion, BigDecimal precio, UUID impuestoId,
            ModoCobro modoCobro, UUID productoId) {
        aplicar(nombre, descripcion, precio, impuestoId, modoCobro, productoId);
    }

    private void aplicar(String nombre, String descripcion, BigDecimal precio, UUID impuestoId,
            ModoCobro modoCobro, UUID productoId) {
        this.nombre = nombre;
        this.descripcion = descripcion == null || descripcion.isBlank() ? null : descripcion.trim();
        this.precio = precio == null || precio.signum() < 0 ? BigDecimal.ZERO : precio;
        this.impuestoId = impuestoId;
        this.modoCobro = modoCobro == null ? ModoCobro.POR_ESTANCIA : modoCobro;
        this.productoId = productoId;
    }

    public void desactivar() {
        this.activo = false;
    }

    /** Consumir este servicio mueve stock. */
    public boolean descuentaInventario() {
        return productoId != null;
    }

    /** Cuántas unidades se cobran para {@code personas} y {@code noches} (o una cantidad fija). */
    public int unidadesPara(int personas, int noches, int cantidad) {
        return modoCobro.unidades(personas, noches, cantidad);
    }

    /** El total del servicio: precio × unidades, con escala de 4 como el resto del dinero. */
    public BigDecimal subtotalPara(int personas, int noches, int cantidad) {
        return precio.multiply(BigDecimal.valueOf(unidadesPara(personas, noches, cantidad)))
                .setScale(4, java.math.RoundingMode.HALF_UP);
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

    public String getDescripcion() {
        return descripcion;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public UUID getImpuestoId() {
        return impuestoId;
    }

    public ModoCobro getModoCobro() {
        return modoCobro;
    }

    public UUID getProductoId() {
        return productoId;
    }

    public boolean isActivo() {
        return activo;
    }
}
