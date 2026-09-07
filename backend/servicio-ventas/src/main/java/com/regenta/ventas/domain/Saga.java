package com.regenta.ventas.domain;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * La máquina de estados de una saga, persistida. HU-038.
 *
 * <p>Vive en la tabla, no en memoria: si el servicio se reinicia con sagas en
 * curso, una instancia nueva las retoma desde aquí (criterio 5). El ciclo para
 * "descontar stock de una venta" es
 * {@code ESPERANDO_STOCK → COMPLETADA} (llegó {@code stock_reservado}) o
 * {@code ESPERANDO_STOCK → COMPENSADA} (llegó {@code stock_reserva_fallida} o
 * venció el timeout).
 */
@Entity
@Table(name = "sagas")
public class Saga {

    public static final String TIPO_VENTA_DESCUENTA_STOCK = "VENTA_DESCUENTA_STOCK";

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(nullable = false, length = 40, updatable = false)
    private String tipo;

    @Column(name = "correlacion_id", nullable = false, updatable = false)
    private UUID correlacionId;

    @Column(name = "agregado_id", nullable = false, updatable = false)
    private UUID agregadoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoSaga estado;

    @Column(name = "paso_actual", length = 40)
    private String pasoActual;

    @Column(nullable = false)
    private int intentos;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload = new LinkedHashMap<>();

    @Column(name = "ultimo_error", columnDefinition = "text")
    private String ultimoError;

    @Column(name = "timeout_en")
    private OffsetDateTime timeoutEn;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    protected Saga() {
    }

    public static Saga paraVenta(UUID negocioId, UUID ventaId, OffsetDateTime timeoutEn) {
        Saga s = new Saga();
        s.id = UUID.randomUUID();
        s.negocioId = negocioId;
        s.tipo = TIPO_VENTA_DESCUENTA_STOCK;
        s.correlacionId = UUID.randomUUID();
        s.agregadoId = ventaId;
        s.estado = EstadoSaga.ESPERANDO_STOCK;
        s.pasoActual = "solicitar_reserva_stock";
        s.timeoutEn = timeoutEn;
        return s;
    }

    public boolean estaEsperando() {
        return estado == EstadoSaga.ESPERANDO_STOCK;
    }

    public void guardarPayload(Map<String, Object> payload) {
        this.payload = payload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(payload);
    }

    public void completar() {
        this.estado = EstadoSaga.COMPLETADA;
        this.pasoActual = "venta_completada";
    }

    public void compensar(String motivo) {
        this.estado = EstadoSaga.COMPENSADA;
        this.pasoActual = "compensada";
        this.ultimoError = motivo;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getCorrelacionId() {
        return correlacionId;
    }

    public UUID getAgregadoId() {
        return agregadoId;
    }

    public EstadoSaga getEstado() {
        return estado;
    }
}
