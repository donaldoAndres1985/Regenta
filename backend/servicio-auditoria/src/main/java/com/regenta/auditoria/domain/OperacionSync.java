package com.regenta.auditoria.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Una operación que el cliente hizo sin conexión y sube en un lote (HU-102).
 * El id lo genera el cliente: tiene que poder referenciarla localmente antes
 * de hablar con el servidor.
 */
@Entity
@Table(name = "operaciones_sync")
public class OperacionSync {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "dispositivo_id", nullable = false, updatable = false)
    private UUID dispositivoId;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 120)
    private String idempotencyKey;

    @Column(name = "secuencia_local", nullable = false, updatable = false)
    private long secuenciaLocal;

    @Column(name = "entidad_tipo", nullable = false, updatable = false, length = 60)
    private String entidadTipo;

    @Column(name = "entidad_id", nullable = false, updatable = false)
    private UUID entidadId;

    @Column(nullable = false, updatable = false, length = 10)
    private String operacion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false)
    private String payload;

    @Column(name = "version_base", updatable = false)
    private Long versionBase;

    @Column(name = "creado_cliente_en", nullable = false, updatable = false)
    private OffsetDateTime creadoClienteEn;

    @Column(name = "recibido_en", nullable = false, updatable = false)
    private OffsetDateTime recibidoEn;

    @Column(name = "aplicado_en")
    private OffsetDateTime aplicadoEn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoOperacion estado;

    @Column(nullable = false)
    private short intentos;

    @Column
    private String error;

    protected OperacionSync() {
    }

    public static OperacionSync recibida(UUID id, UUID negocioId, UUID dispositivoId, UUID usuarioId,
            String idempotencyKey, long secuenciaLocal, String entidadTipo, UUID entidadId,
            String operacion, String payload, Long versionBase, OffsetDateTime creadoClienteEn,
            OffsetDateTime ahora) {
        OperacionSync o = new OperacionSync();
        o.id = id;
        o.negocioId = negocioId;
        o.dispositivoId = dispositivoId;
        o.usuarioId = usuarioId;
        o.idempotencyKey = idempotencyKey;
        o.secuenciaLocal = secuenciaLocal;
        o.entidadTipo = entidadTipo;
        o.entidadId = entidadId;
        o.operacion = operacion;
        o.payload = payload;
        o.versionBase = versionBase;
        o.creadoClienteEn = creadoClienteEn;
        o.recibidoEn = ahora;
        o.estado = EstadoOperacion.RECIBIDA;
        return o;
    }

    /** Criterio 4: RECHAZADA con el motivo, sin tocar ninguna otra operación de la cola. */
    public void rechazar(String motivo, OffsetDateTime ahora) {
        this.estado = EstadoOperacion.RECHAZADA;
        this.error = motivo;
        this.aplicadoEn = ahora;
    }

    public void aplicar(OffsetDateTime ahora) {
        this.estado = EstadoOperacion.APLICADA;
        this.aplicadoEn = ahora;
    }

    public void marcarConflicto(String motivo, OffsetDateTime ahora) {
        this.estado = EstadoOperacion.CONFLICTO;
        this.error = motivo;
        this.aplicadoEn = ahora;
    }

    public UUID getId() {
        return id;
    }

    public long getSecuenciaLocal() {
        return secuenciaLocal;
    }

    public EstadoOperacion getEstado() {
        return estado;
    }

    public String getError() {
        return error;
    }
}
