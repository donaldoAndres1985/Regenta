package com.regenta.auditoria.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Un conflicto detectado al aplicar una operación de sincronización (HU-103).
 * Lo detecta el servicio dueño de la entidad —él es quien conoce la versión
 * actual—; aquí solo se registra, se muestra lado a lado y se resuelve.
 */
@Entity
@Table(name = "conflictos_sync")
public class ConflictoSync {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "operacion_id", nullable = false, updatable = false)
    private UUID operacionId;

    @Column(name = "entidad_tipo", nullable = false, updatable = false, length = 60)
    private String entidadTipo;

    @Column(name = "entidad_id", nullable = false, updatable = false)
    private UUID entidadId;

    @Column(nullable = false, updatable = false, length = 30)
    private String tipo;

    @Column(name = "version_servidor", updatable = false)
    private Long versionServidor;

    @Column(name = "version_cliente", updatable = false)
    private Long versionCliente;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_servidor", updatable = false)
    private String datosServidor;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_cliente", updatable = false)
    private String datosCliente;

    @Column(length = 25)
    private String resolucion;

    @Column(name = "resuelto_por")
    private UUID resueltoPor;

    @Column(name = "resuelto_en")
    private OffsetDateTime resueltoEn;

    @Column(name = "detectado_en", nullable = false, updatable = false)
    private OffsetDateTime detectadoEn;

    protected ConflictoSync() {
    }

    public static ConflictoSync detectado(UUID negocioId, UUID operacionId, String entidadTipo,
            UUID entidadId, String tipo, Long versionServidor, Long versionCliente, String datosServidor,
            String datosCliente, OffsetDateTime ahora) {
        ConflictoSync c = new ConflictoSync();
        c.id = UUID.randomUUID();
        c.negocioId = negocioId;
        c.operacionId = operacionId;
        c.entidadTipo = entidadTipo;
        c.entidadId = entidadId;
        c.tipo = tipo;
        c.versionServidor = versionServidor;
        c.versionCliente = versionCliente;
        c.datosServidor = datosServidor;
        c.datosCliente = datosCliente;
        c.detectadoEn = ahora;
        return c;
    }

    /** Criterio 3: queda quién resolvió y cómo. */
    public void resolver(String resolucion, UUID resueltoPor, OffsetDateTime ahora) {
        this.resolucion = resolucion;
        this.resueltoPor = resueltoPor;
        this.resueltoEn = ahora;
    }

    public boolean estaResuelto() {
        return resolucion != null;
    }

    public UUID getId() {
        return id;
    }

    public String getEntidadTipo() {
        return entidadTipo;
    }

    public UUID getEntidadId() {
        return entidadId;
    }

    public String getTipo() {
        return tipo;
    }

    public String getDatosServidor() {
        return datosServidor;
    }

    public String getDatosCliente() {
        return datosCliente;
    }

    public String getResolucion() {
        return resolucion;
    }

    public OffsetDateTime getDetectadoEn() {
        return detectadoEn;
    }
}
