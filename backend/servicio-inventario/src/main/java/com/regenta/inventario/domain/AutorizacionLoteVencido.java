package com.regenta.inventario.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * La constancia de que alguien autorizo dar salida a un lote vencido. HU-031,
 * criterio 4.
 *
 * <p>Append-only: un trigger de la base rechaza UPDATE y DELETE. Una
 * autorizacion no se borra.
 */
@Entity
@Table(name = "autorizaciones_lote_vencido")
public class AutorizacionLoteVencido {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "lote_id", nullable = false, updatable = false)
    private UUID loteId;

    @Column(name = "producto_id", nullable = false, updatable = false)
    private UUID productoId;

    @Column(name = "bodega_id", nullable = false, updatable = false)
    private UUID bodegaId;

    @Column(nullable = false, updatable = false)
    private BigDecimal cantidad;

    @Column(name = "usuario_id", updatable = false)
    private UUID usuarioId;

    @Column(nullable = false, columnDefinition = "text", updatable = false)
    private String motivo;

    @Column(name = "fecha_vencimiento", nullable = false, updatable = false)
    private LocalDate fechaVencimiento;

    @CreationTimestamp
    @Column(name = "autorizado_en", nullable = false, updatable = false)
    private OffsetDateTime autorizadoEn;

    protected AutorizacionLoteVencido() {
    }

    public static AutorizacionLoteVencido de(UUID negocioId, Lote lote, UUID bodegaId,
            BigDecimal cantidad, UUID usuarioId, String motivo) {
        AutorizacionLoteVencido a = new AutorizacionLoteVencido();
        a.id = UUID.randomUUID();
        a.negocioId = negocioId;
        a.loteId = lote.getId();
        a.productoId = lote.getProductoId();
        a.bodegaId = bodegaId;
        a.cantidad = cantidad;
        a.usuarioId = usuarioId;
        a.motivo = motivo;
        a.fechaVencimiento = lote.getFechaVencimiento();
        return a;
    }

    public UUID getId() {
        return id;
    }

    public UUID getLoteId() {
        return loteId;
    }

    public String getMotivo() {
        return motivo;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }
}
