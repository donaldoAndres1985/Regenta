package com.regenta.inventario.domain;

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
 * Stock apartado para una venta que todavía no se concreta. HU-034.
 *
 * <p>Mientras está {@code ACTIVA} sube {@code existencias.cantidad_reservada}
 * sin tocar {@code cantidad}. Si la venta se confirma pasa a {@code CONFIRMADA}
 * y se convierte en salida real; si expira, el barrido la pasa a
 * {@code EXPIRADA} y libera la cantidad reservada.
 */
@Entity
@Table(name = "reservas_stock")
public class ReservaStock {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "producto_id", nullable = false, updatable = false)
    private UUID productoId;

    @Column(name = "bodega_id", nullable = false, updatable = false)
    private UUID bodegaId;

    @Column(nullable = false, updatable = false)
    private BigDecimal cantidad;

    @Column(name = "origen_tipo", nullable = false, length = 20, updatable = false)
    private String origenTipo;

    @Column(name = "origen_id", nullable = false, updatable = false)
    private UUID origenId;

    @Column(name = "correlacion_id", nullable = false, updatable = false)
    private UUID correlacionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReserva estado;

    @Column(name = "expira_en", nullable = false, updatable = false)
    private OffsetDateTime expiraEn;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    protected ReservaStock() {
    }

    public static ReservaStock nueva(UUID negocioId, UUID productoId, UUID bodegaId,
            BigDecimal cantidad, String origenTipo, UUID origenId, UUID correlacionId,
            OffsetDateTime expiraEn) {
        ReservaStock r = new ReservaStock();
        r.id = UUID.randomUUID();
        r.negocioId = negocioId;
        r.productoId = productoId;
        r.bodegaId = bodegaId;
        r.cantidad = cantidad;
        r.origenTipo = origenTipo;
        r.origenId = origenId;
        r.correlacionId = correlacionId;
        r.estado = EstadoReserva.ACTIVA;
        r.expiraEn = expiraEn;
        return r;
    }

    public boolean estaActiva() {
        return estado == EstadoReserva.ACTIVA;
    }

    public void confirmar() {
        this.estado = EstadoReserva.CONFIRMADA;
    }

    public void expirar() {
        this.estado = EstadoReserva.EXPIRADA;
    }

    public void liberar() {
        this.estado = EstadoReserva.LIBERADA;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getProductoId() {
        return productoId;
    }

    public UUID getBodegaId() {
        return bodegaId;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public String getOrigenTipo() {
        return origenTipo;
    }

    public UUID getOrigenId() {
        return origenId;
    }

    public UUID getCorrelacionId() {
        return correlacionId;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public OffsetDateTime getExpiraEn() {
        return expiraEn;
    }
}
