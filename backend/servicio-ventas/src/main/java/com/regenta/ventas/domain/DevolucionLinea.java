package com.regenta.ventas.domain;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Una línea de una devolución: apunta a la línea de la venta original. HU-042. */
@Entity
@Table(name = "devolucion_lineas")
public class DevolucionLinea {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "devolucion_id", nullable = false, updatable = false)
    private UUID devolucionId;

    @Column(name = "venta_linea_id", nullable = false, updatable = false)
    private UUID ventaLineaId;

    @Column(name = "producto_id", nullable = false, updatable = false)
    private UUID productoId;

    @Column(nullable = false, updatable = false)
    private BigDecimal cantidad;

    @Column(nullable = false, updatable = false)
    private BigDecimal monto;

    protected DevolucionLinea() {
    }

    public static DevolucionLinea de(UUID negocioId, UUID devolucionId, UUID ventaLineaId,
            UUID productoId, BigDecimal cantidad, BigDecimal monto) {
        DevolucionLinea l = new DevolucionLinea();
        l.id = UUID.randomUUID();
        l.negocioId = negocioId;
        l.devolucionId = devolucionId;
        l.ventaLineaId = ventaLineaId;
        l.productoId = productoId;
        l.cantidad = cantidad;
        l.monto = monto;
        return l;
    }

    public UUID getProductoId() {
        return productoId;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public BigDecimal getMonto() {
        return monto;
    }
}
