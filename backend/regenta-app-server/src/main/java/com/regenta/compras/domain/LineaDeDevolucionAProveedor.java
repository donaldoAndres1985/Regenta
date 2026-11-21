package com.regenta.compras.domain;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * HU-178 · Una línea devuelta: de qué línea de la recepción sale, cuánto, de qué lote, a qué costo de
 * entrada sale del inventario y cuánto vale en la nota crédito (base con sus descuentos + IVA).
 */
@Entity
@Table(name = "devolucion_lineas", schema = "compras")
public class LineaDeDevolucionAProveedor {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "devolucion_id", nullable = false, updatable = false)
    private UUID devolucionId;

    @Column(name = "recepcion_linea_id", nullable = false, updatable = false)
    private UUID recepcionLineaId;

    @Column(name = "producto_id", nullable = false, updatable = false)
    private UUID productoId;

    @Column(name = "nombre_snapshot", nullable = false, length = 200, updatable = false)
    private String nombre;

    @Column(nullable = false, updatable = false)
    private BigDecimal cantidad;

    @Column(name = "codigo_lote", length = 60, updatable = false)
    private String codigoLote;

    @Column(name = "costo_entrada", nullable = false, updatable = false)
    private BigDecimal costoEntrada;

    @Column(nullable = false, updatable = false)
    private BigDecimal base;

    @Column(nullable = false, updatable = false)
    private BigDecimal impuesto;

    protected LineaDeDevolucionAProveedor() {
    }

    public static LineaDeDevolucionAProveedor de(UUID negocioId, UUID devolucionId, RecepcionLinea origen, BigDecimal cantidad,
            BigDecimal base, BigDecimal impuesto) {
        LineaDeDevolucionAProveedor l = new LineaDeDevolucionAProveedor();
        l.id = UUID.randomUUID();
        l.negocioId = negocioId;
        l.devolucionId = devolucionId;
        l.recepcionLineaId = origen.getId();
        l.productoId = origen.getProductoId();
        l.nombre = origen.getNombre() == null ? "Producto" : origen.getNombre();
        l.cantidad = cantidad;
        l.codigoLote = origen.getCodigoLote();
        l.costoEntrada = origen.getCostoEntrada() == null ? origen.costoNeto() : origen.getCostoEntrada();
        l.base = base;
        l.impuesto = impuesto;
        return l;
    }

    public UUID getId() {
        return id;
    }

    public UUID getDevolucionId() {
        return devolucionId;
    }

    public UUID getRecepcionLineaId() {
        return recepcionLineaId;
    }

    public UUID getProductoId() {
        return productoId;
    }

    public String getNombre() {
        return nombre;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public String getCodigoLote() {
        return codigoLote;
    }

    public BigDecimal getCostoEntrada() {
        return costoEntrada;
    }

    public BigDecimal getBase() {
        return base;
    }

    public BigDecimal getImpuesto() {
        return impuesto;
    }
}
