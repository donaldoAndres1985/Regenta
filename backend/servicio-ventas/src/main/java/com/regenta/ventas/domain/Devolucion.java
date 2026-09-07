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
 * Una devolución de parte o de toda una venta. HU-042.
 *
 * <p>{@code tipo} = TOTAL si deja la venta completamente devuelta, PARCIAL si no.
 * {@code reintegra_stock} = false para el producto defectuoso: la mercancía no
 * vuelve al inventario pero queda la constancia del motivo (criterio 4).
 */
@Entity
@Table(name = "devoluciones")
public class Devolucion {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "venta_id", nullable = false, updatable = false)
    private UUID ventaId;

    @Column(nullable = false, length = 30, updatable = false)
    private String numero;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40, updatable = false)
    private MotivoDevolucion motivo;

    @Column(columnDefinition = "text", updatable = false)
    private String detalle;

    @Column(name = "reintegra_stock", nullable = false, updatable = false)
    private boolean reintegraStock;

    @Column(name = "bodega_destino_id", updatable = false)
    private UUID bodegaDestinoId;

    @Column(nullable = false)
    private BigDecimal total;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "usuario_id", updatable = false)
    private UUID usuarioId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime fecha;

    protected Devolucion() {
    }

    public static Devolucion crear(UUID negocioId, UUID ventaId, String numero,
            MotivoDevolucion motivo, String detalle, boolean reintegraStock, UUID bodegaDestinoId,
            UUID usuarioId) {
        Devolucion d = new Devolucion();
        d.id = UUID.randomUUID();
        d.negocioId = negocioId;
        d.ventaId = ventaId;
        d.numero = numero;
        d.motivo = motivo;
        d.detalle = detalle == null || detalle.isBlank() ? null : detalle.trim();
        d.reintegraStock = reintegraStock;
        d.bodegaDestinoId = bodegaDestinoId;
        d.usuarioId = usuarioId;
        d.tipo = "PARCIAL";
        d.estado = "REGISTRADA";
        d.total = BigDecimal.ZERO;
        return d;
    }

    public void cerrar(BigDecimal total, boolean dejaLaVentaTotalmenteDevuelta) {
        this.total = total;
        this.tipo = dejaLaVentaTotalmenteDevuelta ? "TOTAL" : "PARCIAL";
    }

    public UUID getId() {
        return id;
    }

    public boolean isReintegraStock() {
        return reintegraStock;
    }

    public UUID getBodegaDestinoId() {
        return bodegaDestinoId;
    }

    public MotivoDevolucion getMotivo() {
        return motivo;
    }

    public String getNumero() {
        return numero;
    }

    public BigDecimal getTotal() {
        return total;
    }
}
