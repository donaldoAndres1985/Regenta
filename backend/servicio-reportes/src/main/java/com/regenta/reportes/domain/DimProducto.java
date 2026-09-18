package com.regenta.reportes.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * La dimensión producto, SCD tipo 2 (HU-096 criterios 2 y 3): si cambia el
 * nombre o la categoría de un producto, esta versión se cierra
 * ({@code vigente_hasta}, {@code es_actual = false}) y se abre una nueva. Un
 * reporte de hace tres meses sigue apuntando a la versión que existía
 * entonces, así que su nombre no se mueve retroactivamente.
 */
@Entity
@Table(name = "dim_producto")
public class DimProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sk;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "producto_id", nullable = false, updatable = false)
    private UUID productoId;

    @Column(length = 60)
    private String sku;

    @Column(nullable = false, length = 180)
    private String nombre;

    @Column(name = "categoria_id")
    private UUID categoriaId;

    @Column(name = "categoria_nombre", length = 100)
    private String categoriaNombre;

    @Column(name = "precio_venta")
    private BigDecimal precioVenta;

    @Column
    private BigDecimal costo;

    @Column(name = "vigente_desde", nullable = false)
    private OffsetDateTime vigenteDesde;

    @Column(name = "vigente_hasta")
    private OffsetDateTime vigenteHasta;

    @Column(name = "es_actual", nullable = false)
    private boolean esActual;

    protected DimProducto() {
    }

    public static DimProducto nuevaVersion(UUID negocioId, UUID productoId, String sku, String nombre,
            UUID categoriaId, String categoriaNombre, BigDecimal precioVenta, BigDecimal costo) {
        DimProducto d = new DimProducto();
        d.negocioId = negocioId;
        d.productoId = productoId;
        d.sku = sku;
        d.nombre = nombre;
        d.categoriaId = categoriaId;
        d.categoriaNombre = categoriaNombre;
        d.precioVenta = precioVenta;
        d.costo = costo;
        d.vigenteDesde = OffsetDateTime.now();
        d.esActual = true;
        return d;
    }

    /** Si algo que se le muestra a un reporte cambió, hace falta una versión nueva. */
    public boolean cambioFrenteA(String nombre, String categoriaNombre, BigDecimal precioVenta,
            BigDecimal costo) {
        return !Objects.equals(this.nombre, nombre)
                || !Objects.equals(this.categoriaNombre, categoriaNombre)
                || !igualNumerico(this.precioVenta, precioVenta)
                || !igualNumerico(this.costo, costo);
    }

    private static boolean igualNumerico(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.compareTo(b) == 0;
    }

    /** Cierra esta versión: deja de ser la actual (HU-096 criterio 2). */
    public void cerrar() {
        this.esActual = false;
        this.vigenteHasta = OffsetDateTime.now();
    }

    public Long getSk() {
        return sk;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getProductoId() {
        return productoId;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean isEsActual() {
        return esActual;
    }

    public OffsetDateTime getVigenteHasta() {
        return vigenteHasta;
    }
}
