package com.regenta.ventas.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Un pago aplicado a una venta. HU-039.
 *
 * <p>Una venta puede tener varios (pago mixto: parte en efectivo, parte con
 * tarjeta). {@code monto} es lo que se aplica al saldo; {@code monto_recibido}
 * es lo que entregó el cliente en efectivo y {@code cambio} la diferencia.
 */
@Entity
@Table(name = "pagos_venta")
public class PagoDeVenta {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "venta_id", nullable = false, updatable = false)
    private UUID ventaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private MetodoDePago metodo;

    @Column(nullable = false, updatable = false)
    private BigDecimal monto;

    @Column(name = "monto_recibido", updatable = false)
    private BigDecimal montoRecibido;

    @Column(nullable = false, updatable = false)
    private BigDecimal cambio;

    @Column(length = 80, updatable = false)
    private String referencia;

    @Column(length = 30, updatable = false)
    private String franquicia;

    @CreationTimestamp
    @Column(name = "recibido_en", nullable = false, updatable = false)
    private OffsetDateTime recibidoEn;

    protected PagoDeVenta() {
    }

    public static PagoDeVenta de(UUID negocioId, UUID ventaId, MetodoDePago metodo, BigDecimal monto,
            BigDecimal montoRecibido, BigDecimal cambio, String referencia, String franquicia) {
        PagoDeVenta p = new PagoDeVenta();
        p.id = UUID.randomUUID();
        p.negocioId = negocioId;
        p.ventaId = ventaId;
        p.metodo = metodo;
        p.monto = monto;
        p.montoRecibido = montoRecibido;
        p.cambio = cambio == null ? BigDecimal.ZERO : cambio;
        p.referencia = referencia == null || referencia.isBlank() ? null : referencia.trim();
        p.franquicia = franquicia == null || franquicia.isBlank() ? null : franquicia.trim();
        return p;
    }

    public UUID getId() {
        return id;
    }

    public MetodoDePago getMetodo() {
        return metodo;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public BigDecimal getCambio() {
        return cambio;
    }
}
