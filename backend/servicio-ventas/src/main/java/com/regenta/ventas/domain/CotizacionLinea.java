package com.regenta.ventas.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Una línea de una cotización. Mismo snapshot que la línea de venta. HU-044. */
@Entity
@Table(name = "cotizacion_lineas")
public class CotizacionLinea {

    private static final int ESCALA = 4;
    private static final BigDecimal CIEN = new BigDecimal("100");

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "cotizacion_id", nullable = false, updatable = false)
    private UUID cotizacionId;

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

    @Column(nullable = false, updatable = false)
    private BigDecimal cantidad;

    @Column(name = "precio_unitario", nullable = false, updatable = false)
    private BigDecimal precioUnitario;

    @Column(name = "descuento_pct", nullable = false, updatable = false)
    private BigDecimal descuentoPct;

    @Column(name = "impuesto_codigo", length = 20, updatable = false)
    private String impuestoCodigo;

    @Column(name = "impuesto_pct", nullable = false, updatable = false)
    private BigDecimal impuestoPct;

    @Column(name = "costo_unitario_snapshot", nullable = false, updatable = false)
    private BigDecimal costoUnitarioSnapshot;

    @Column(nullable = false, updatable = false)
    private BigDecimal total;

    protected CotizacionLinea() {
    }

    public static CotizacionLinea de(UUID negocioId, UUID cotizacionId, short linea, UUID productoId,
            String sku, String nombre, String unidad, BigDecimal cantidad, BigDecimal precioUnitario,
            BigDecimal descuentoPct, String impuestoCodigo, BigDecimal impuestoPct,
            BigDecimal costoUnitario) {
        CotizacionLinea l = new CotizacionLinea();
        l.id = UUID.randomUUID();
        l.negocioId = negocioId;
        l.cotizacionId = cotizacionId;
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
        BigDecimal bruto = l.precioUnitario.multiply(l.cantidad);
        BigDecimal base = bruto.subtract(bruto.multiply(l.descuentoPct).divide(CIEN, ESCALA + 2,
                RoundingMode.HALF_UP));
        BigDecimal impuesto = base.multiply(l.impuestoPct).divide(CIEN, ESCALA + 2,
                RoundingMode.HALF_UP);
        l.total = base.add(impuesto).setScale(ESCALA, RoundingMode.HALF_UP);
        return l;
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

    public String getUnidadSnapshot() {
        return unidadSnapshot;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public BigDecimal getDescuentoPct() {
        return descuentoPct;
    }

    public String getImpuestoCodigo() {
        return impuestoCodigo;
    }

    public BigDecimal getImpuestoPct() {
        return impuestoPct;
    }

    public BigDecimal getCostoUnitarioSnapshot() {
        return costoUnitarioSnapshot;
    }

    public BigDecimal getTotal() {
        return total;
    }
}
