package com.regenta.compras.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Un renglón de una orden de compra: qué producto, cuánto se pide, a qué costo.
 *
 * <p>{@code cantidad_recibida} arranca en cero y la va subiendo la recepción
 * (HU-048). {@link #faltante()} —lo pedido menos lo recibido— es lo que
 * responde el criterio 4 de HU-047.
 */
@Entity
@Table(name = "orden_compra_lineas")
public class OrdenCompraLinea {

    private static final BigDecimal CIEN = new BigDecimal("100");

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "orden_id", nullable = false, updatable = false)
    private UUID ordenId;

    @Column(nullable = false)
    private short linea;

    @Column(name = "producto_id", nullable = false)
    private UUID productoId;

    @Column(name = "nombre_snapshot", nullable = false, length = 180)
    private String nombreSnapshot;

    @Column(name = "cantidad_pedida", nullable = false)
    private BigDecimal cantidadPedida;

    @Column(name = "cantidad_recibida", nullable = false)
    private BigDecimal cantidadRecibida;

    @Column(name = "costo_unitario", nullable = false)
    private BigDecimal costoUnitario;

    @Column(name = "descuento_pct", nullable = false)
    private BigDecimal descuentoPct;

    @Column(name = "impuesto_pct", nullable = false)
    private BigDecimal impuestoPct;

    @Column(nullable = false)
    private BigDecimal subtotal;

    @Column(nullable = false)
    private BigDecimal total;

    protected OrdenCompraLinea() {
    }

    public static OrdenCompraLinea nueva(UUID ordenId, UUID negocioId, int linea, UUID productoId,
            String nombreSnapshot, BigDecimal cantidadPedida, BigDecimal costoUnitario,
            BigDecimal descuentoPct, BigDecimal impuestoPct) {
        if (cantidadPedida == null || cantidadPedida.signum() <= 0) {
            throw new IllegalArgumentException("La cantidad pedida debe ser mayor que cero");
        }
        if (costoUnitario == null || costoUnitario.signum() < 0) {
            throw new IllegalArgumentException("El costo unitario no puede ser negativo");
        }
        OrdenCompraLinea l = new OrdenCompraLinea();
        l.id = UUID.randomUUID();
        l.negocioId = negocioId;
        l.ordenId = ordenId;
        l.linea = (short) linea;
        l.productoId = productoId;
        l.nombreSnapshot = nombreSnapshot;
        l.cantidadPedida = cantidadPedida;
        l.cantidadRecibida = BigDecimal.ZERO;
        l.costoUnitario = costoUnitario;
        l.descuentoPct = descuentoPct == null ? BigDecimal.ZERO : descuentoPct;
        l.impuestoPct = impuestoPct == null ? BigDecimal.ZERO : impuestoPct;
        l.recalcular();
        return l;
    }

    private void recalcular() {
        BigDecimal bruto = cantidadPedida.multiply(costoUnitario);
        BigDecimal descuento = bruto.multiply(descuentoPct).divide(CIEN, 4, RoundingMode.HALF_UP);
        BigDecimal base = bruto.subtract(descuento);
        BigDecimal impuesto = base.multiply(impuestoPct).divide(CIEN, 4, RoundingMode.HALF_UP);
        this.subtotal = base.setScale(4, RoundingMode.HALF_UP);
        this.total = base.add(impuesto).setScale(4, RoundingMode.HALF_UP);
    }

    public BigDecimal brutoDeLinea() {
        return cantidadPedida.multiply(costoUnitario).setScale(4, RoundingMode.HALF_UP);
    }

    public BigDecimal descuentoDeLinea() {
        return cantidadPedida.multiply(costoUnitario)
                .multiply(descuentoPct).divide(CIEN, 4, RoundingMode.HALF_UP);
    }

    public BigDecimal impuestoDeLinea() {
        return total.subtract(subtotal);
    }

    /** Criterio 4: lo pedido menos lo recibido. Nunca negativo. */
    public BigDecimal faltante() {
        BigDecimal falta = cantidadPedida.subtract(cantidadRecibida);
        return falta.signum() < 0 ? BigDecimal.ZERO : falta;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getOrdenId() {
        return ordenId;
    }

    public int getLinea() {
        return linea;
    }

    public UUID getProductoId() {
        return productoId;
    }

    public String getNombreSnapshot() {
        return nombreSnapshot;
    }

    public BigDecimal getCantidadPedida() {
        return cantidadPedida;
    }

    public BigDecimal getCantidadRecibida() {
        return cantidadRecibida;
    }

    public BigDecimal getCostoUnitario() {
        return costoUnitario;
    }

    public BigDecimal getDescuentoPct() {
        return descuentoPct;
    }

    public BigDecimal getImpuestoPct() {
        return impuestoPct;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getTotal() {
        return total;
    }
}
