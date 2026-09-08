package com.regenta.alertas.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Una alerta encaminada a un usuario por un canal (HU-092 la deja
 * {@code PENDIENTE}; el envío real —push, correo— y el reintento con backoff son
 * HU-094).
 */
@Entity
@Table(name = "entregas")
public class Entrega {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "alerta_id", nullable = false, updatable = false)
    private UUID alertaId;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private CanalDeAlerta canal;

    @Column(length = 255)
    private String destino;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "proveedor_id", length = 120)
    private String proveedorId;

    @Column(nullable = false)
    private short intentos;

    @Column(columnDefinition = "text")
    private String error;

    @Column(name = "enviada_en")
    private OffsetDateTime enviadaEn;

    @Column(name = "leida_en")
    private OffsetDateTime leidaEn;

    protected Entrega() {
    }

    public static Entrega pendiente(UUID negocioId, UUID alertaId, UUID usuarioId,
            CanalDeAlerta canal) {
        Entrega e = new Entrega();
        e.id = UUID.randomUUID();
        e.negocioId = negocioId;
        e.alertaId = alertaId;
        e.usuarioId = usuarioId;
        e.canal = canal;
        e.estado = "PENDIENTE";
        e.intentos = 0;
        return e;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAlertaId() {
        return alertaId;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public CanalDeAlerta getCanal() {
        return canal;
    }

    public String getEstado() {
        return estado;
    }
}
