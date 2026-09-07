package com.regenta.inventario.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Una linea del libro mayor de inventario. FUENTE DE VERDAD del stock (HU-030).
 *
 * <p>Append-only: una vez escrita no se toca. Un error se corrige con un
 * movimiento contrario, nunca editando este. Lo hace cumplir un trigger de la
 * base (V3), no el codigo.
 */
@Entity
@Table(name = "movimientos_inventario")
@IdClass(MovimientoInventarioId.class)
public class MovimientoInventario {

    @Id
    private UUID id;

    @Id
    @Column(name = "ocurrido_en", nullable = false, updatable = false)
    private OffsetDateTime ocurridoEn;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "producto_id", nullable = false, updatable = false)
    private UUID productoId;

    @Column(name = "bodega_id", nullable = false, updatable = false)
    private UUID bodegaId;

    @Column(name = "lote_id", updatable = false)
    private UUID loteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25, updatable = false)
    private TipoMovimiento tipo;

    @Column(nullable = false, updatable = false)
    private short signo;

    @Column(nullable = false, updatable = false)
    private BigDecimal cantidad;

    @Column(name = "costo_unitario", nullable = false, updatable = false)
    private BigDecimal costoUnitario;

    @Column(name = "saldo_posterior", nullable = false, updatable = false)
    private BigDecimal saldoPosterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen_tipo", nullable = false, length = 25, updatable = false)
    private OrigenMovimiento origenTipo;

    @Column(name = "origen_id", updatable = false)
    private UUID origenId;

    @Column(name = "usuario_id", updatable = false)
    private UUID usuarioId;

    @Column(columnDefinition = "text", updatable = false)
    private String motivo;

    @Column(name = "idempotency_key", nullable = false, length = 120, updatable = false)
    private String idempotencyKey;

    protected MovimientoInventario() {
    }

    public static MovimientoInventario de(UUID negocioId, UUID productoId, UUID bodegaId,
            UUID loteId, TipoMovimiento tipo, BigDecimal cantidad, BigDecimal saldoPosterior,
            OrigenMovimiento origenTipo, UUID origenId, UUID usuarioId, String motivo,
            String idempotencyKey, OffsetDateTime ocurridoEn) {
        MovimientoInventario m = new MovimientoInventario();
        m.id = UUID.randomUUID();
        m.ocurridoEn = ocurridoEn;
        m.negocioId = negocioId;
        m.productoId = productoId;
        m.bodegaId = bodegaId;
        m.loteId = loteId;
        m.tipo = tipo;
        m.signo = (short) tipo.signo();
        m.cantidad = cantidad;
        m.costoUnitario = BigDecimal.ZERO;
        m.saldoPosterior = saldoPosterior;
        m.origenTipo = origenTipo;
        m.origenId = origenId;
        m.usuarioId = usuarioId;
        m.motivo = motivo;
        m.idempotencyKey = idempotencyKey;
        return m;
    }

    public UUID getId() {
        return id;
    }

    public OffsetDateTime getOcurridoEn() {
        return ocurridoEn;
    }

    public UUID getProductoId() {
        return productoId;
    }

    public UUID getBodegaId() {
        return bodegaId;
    }

    public UUID getLoteId() {
        return loteId;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public short getSigno() {
        return signo;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public BigDecimal getSaldoPosterior() {
        return saldoPosterior;
    }

    public OrigenMovimiento getOrigenTipo() {
        return origenTipo;
    }

    public UUID getOrigenId() {
        return origenId;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    /** El aporte con signo al saldo: negativo en salidas. */
    public BigDecimal aporteConSigno() {
        return cantidad.multiply(BigDecimal.valueOf(signo));
    }
}
