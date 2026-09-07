package com.regenta.ventas.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Una línea de una venta. HU-037.
 *
 * <p>Los campos {@code *_snapshot} son una copia del producto al momento de
 * agregarlo: si mañana editan el producto, la línea no cambia (criterios 1 y 2).
 * Los importes de la línea se calculan aquí y se guardan.
 */
@Entity
@Table(name = "venta_lineas")
public class VentaLinea {

    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final int ESCALA = 4;

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "venta_id", nullable = false, updatable = false)
    private UUID ventaId;

    @Column(nullable = false, updatable = false)
    private short linea;

    @Column(name = "producto_id", nullable = false, updatable = false)
    private UUID productoId;

    @Column(name = "sku_snapshot", nullable = false, length = 60, updatable = false)
    private String skuSnapshot;

    @Column(name = "nombre_snapshot", nullable = false, length = 180, updatable = false)
    private String nombreSnapshot;

    @Column(name = "unidad_snapshot", length = 20, updatable = false)
    private String unidadSnapshot;

    @Column(nullable = false)
    private BigDecimal cantidad;

    @Column(name = "precio_unitario", nullable = false, updatable = false)
    private BigDecimal precioUnitario;

    @Column(name = "descuento_pct", nullable = false)
    private BigDecimal descuentoPct;

    @Column(name = "descuento_valor", nullable = false)
    private BigDecimal descuentoValor;

    @Column(name = "impuesto_codigo", length = 20, updatable = false)
    private String impuestoCodigo;

    @Column(name = "impuesto_pct", nullable = false, updatable = false)
    private BigDecimal impuestoPct;

    @Column(name = "impuesto_valor", nullable = false)
    private BigDecimal impuestoValor;

    @Column(name = "base_gravable", nullable = false)
    private BigDecimal baseGravable;

    @Column(nullable = false)
    private BigDecimal subtotal;

    @Column(nullable = false)
    private BigDecimal total;

    @Column(name = "costo_unitario_snapshot", nullable = false, updatable = false)
    private BigDecimal costoUnitarioSnapshot;

    @Column(name = "cantidad_devuelta", nullable = false)
    private BigDecimal cantidadDevuelta;

    protected VentaLinea() {
    }

    public static VentaLinea de(UUID negocioId, UUID ventaId, short linea, UUID productoId,
            String sku, String nombre, String unidad, BigDecimal cantidad, BigDecimal precioUnitario,
            BigDecimal descuentoPct, String impuestoCodigo, BigDecimal impuestoPct,
            BigDecimal costoUnitario) {
        VentaLinea l = new VentaLinea();
        l.id = UUID.randomUUID();
        l.negocioId = negocioId;
        l.ventaId = ventaId;
        l.linea = linea;
        l.productoId = productoId;
        l.skuSnapshot = sku;
        l.nombreSnapshot = nombre;
        l.unidadSnapshot = unidad;
        l.cantidad = cantidad;
        l.precioUnitario = precioUnitario;
        l.descuentoPct = descuentoPct == null ? BigDecimal.ZERO : descuentoPct;
        l.impuestoCodigo = impuestoCodigo;
        l.impuestoPct = impuestoPct == null ? BigDecimal.ZERO : impuestoPct;
        l.costoUnitarioSnapshot = costoUnitario == null ? BigDecimal.ZERO : costoUnitario;
        l.cantidadDevuelta = BigDecimal.ZERO;
        l.calcular();
        return l;
    }

    public void cambiarCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
        calcular();
    }

    /** Suma una devolución. No puede pasar de lo vendido (HU-042, criterio 2). */
    public void registrarDevolucion(BigDecimal cantidad) {
        BigDecimal total = this.cantidadDevuelta.add(cantidad);
        if (total.compareTo(this.cantidad) > 0) {
            throw new com.regenta.comun.errores.ReglaDeNegocioException("No se puede devolver "
                    + cantidad + " de la linea " + linea + ": se vendieron " + this.cantidad
                    + " y ya se devolvieron " + this.cantidadDevuelta);
        }
        this.cantidadDevuelta = total;
    }

    /** Cuánto de esta línea corresponde devolver, con impuesto, por unidad. */
    public BigDecimal montoDevolucionPor(BigDecimal cantidad) {
        return total.divide(this.cantidad, ESCALA + 2, RoundingMode.HALF_UP).multiply(cantidad)
                .setScale(ESCALA, RoundingMode.HALF_UP);
    }

    public boolean totalmenteDevuelta() {
        return cantidadDevuelta.compareTo(cantidad) >= 0;
    }

    public UUID getId() {
        return id;
    }

    public BigDecimal getCantidadDevuelta() {
        return cantidadDevuelta;
    }

    private void calcular() {
        BigDecimal bruto = precioUnitario.multiply(cantidad);
        this.subtotal = escala(bruto);
        this.descuentoValor = escala(bruto.multiply(descuentoPct).divide(CIEN, ESCALA + 2,
                RoundingMode.HALF_UP));
        this.baseGravable = escala(bruto.subtract(descuentoValor));
        this.impuestoValor = escala(baseGravable.multiply(impuestoPct).divide(CIEN, ESCALA + 2,
                RoundingMode.HALF_UP));
        this.total = escala(baseGravable.add(impuestoValor));
    }

    private static BigDecimal escala(BigDecimal v) {
        return v.setScale(ESCALA, RoundingMode.HALF_UP);
    }

    public short getLinea() {
        return linea;
    }

    public UUID getProductoId() {
        return productoId;
    }

    public String getSkuSnapshot() {
        return skuSnapshot;
    }

    public String getNombreSnapshot() {
        return nombreSnapshot;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public BigDecimal getDescuentoValor() {
        return descuentoValor;
    }

    public BigDecimal getImpuestoPct() {
        return impuestoPct;
    }

    public BigDecimal getImpuestoValor() {
        return impuestoValor;
    }

    public BigDecimal getBaseGravable() {
        return baseGravable;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public BigDecimal getCostoUnitarioSnapshot() {
        return costoUnitarioSnapshot;
    }
}
