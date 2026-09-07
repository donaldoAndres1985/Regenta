package com.regenta.inventario.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * El saldo actual de un producto en una bodega. Es una PROYECCION: la verdad
 * esta en {@code movimientos_inventario} (HU-030). {@code cantidad_disponible}
 * lo calcula la base (columna generada): {@code cantidad - cantidad_reservada}.
 */
@Entity
@Table(name = "existencias")
@IdClass(ExistenciaId.class)
public class Existencia {

    @Id
    @Column(name = "producto_id")
    private UUID productoId;

    @Id
    @Column(name = "bodega_id")
    private UUID bodegaId;

    @Column(name = "negocio_id", nullable = false)
    private UUID negocioId;

    @Column(nullable = false)
    private BigDecimal cantidad;

    @Column(name = "cantidad_reservada", nullable = false)
    private BigDecimal cantidadReservada;

    @Column(name = "cantidad_disponible", insertable = false, updatable = false)
    private BigDecimal cantidadDisponible;

    @Column(name = "costo_promedio", nullable = false)
    private BigDecimal costoPromedio;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    @Version
    @Column(nullable = false)
    private long version;

    protected Existencia() {
    }

    public static Existencia enCero(UUID negocioId, UUID productoId, UUID bodegaId) {
        Existencia existencia = new Existencia();
        existencia.negocioId = negocioId;
        existencia.productoId = productoId;
        existencia.bodegaId = bodegaId;
        existencia.cantidad = BigDecimal.ZERO;
        existencia.cantidadReservada = BigDecimal.ZERO;
        existencia.costoPromedio = BigDecimal.ZERO;
        return existencia;
    }

    /** El saldo que quedaria si se aplicara un movimiento, sin aplicarlo. */
    public BigDecimal saldoSiAplico(int signo, BigDecimal cantidad) {
        return this.cantidad.add(cantidad.multiply(BigDecimal.valueOf(signo)));
    }

    /** Mueve la cantidad segun el signo del movimiento (+1 entrada, -1 salida). */
    public void aplicar(int signo, BigDecimal cantidad) {
        this.cantidad = saldoSiAplico(signo, cantidad);
    }

    /** Lo que queda para vender: {@code cantidad - cantidad_reservada}. */
    public BigDecimal disponible() {
        return cantidad.subtract(cantidadReservada);
    }

    /** Aparta stock para una venta en curso (HU-034). No toca {@code cantidad}. */
    public void reservar(BigDecimal cantidad) {
        this.cantidadReservada = this.cantidadReservada.add(cantidad);
    }

    /** Suelta stock apartado: al expirar la reserva, o al convertirla en salida. */
    public void liberarReserva(BigDecimal cantidad) {
        this.cantidadReservada = this.cantidadReservada.subtract(cantidad);
        if (this.cantidadReservada.signum() < 0) {
            this.cantidadReservada = BigDecimal.ZERO;
        }
    }

    public UUID getProductoId() {
        return productoId;
    }

    public UUID getBodegaId() {
        return bodegaId;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public BigDecimal getCantidadReservada() {
        return cantidadReservada;
    }

    public BigDecimal getCantidadDisponible() {
        return cantidadDisponible;
    }

    public BigDecimal getCostoPromedio() {
        return costoPromedio;
    }
}
