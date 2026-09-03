package com.regenta.comun.dominio;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Base de toda entidad multi-tenant.
 *
 * El filtro por negocio_id se aplica en TRES capas independientes:
 *   1. @FilterDef/@Filter de Hibernate (activado por el TenantFilterAspect).
 *   2. Row-Level Security en PostgreSQL (red de seguridad si falla la 1).
 *   3. El claim negocio_id del JWT, validado en el Gateway.
 * Ninguna de las tres es suficiente por si sola.
 */
@MappedSuperclass
public abstract class EntidadTenant {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "creado_por", updatable = false)
    private UUID creadoPor;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    @Column(name = "actualizado_por")
    private UUID actualizadoPor;

    /** Bloqueo optimista: imprescindible con sincronización offline. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "eliminado_en")
    private OffsetDateTime eliminadoEn;

    @PrePersist
    void asignarId() {
        if (id == null) id = UUID.randomUUID();   // en produccion: UUID v7
        if (negocioId == null) negocioId = TenantContext.negocioActual();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getNegocioId() { return negocioId; }
    public void setNegocioId(UUID negocioId) { this.negocioId = negocioId; }
    public Long getVersion() { return version; }
    public boolean estaEliminada() { return eliminadoEn != null; }
    public void eliminar() { this.eliminadoEn = OffsetDateTime.now(); }
}
