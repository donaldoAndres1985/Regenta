package com.regenta.clientes.domain;

import java.time.LocalDate;
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
 * Una llamada, visita o nota sobre un cliente (HU-024): con qué tipo, cuándo
 * pasó, quién la registró y el detalle. Si lleva {@code seguimientoEn}, el
 * barrido avisa al responsable cuando llega la fecha y marca
 * {@code seguimientoNotificadoEn} para no repetir el aviso.
 */
@Entity
@Table(name = "interacciones")
public class Interaccion {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "cliente_id", nullable = false, updatable = false)
    private UUID clienteId;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoInteraccion tipo;

    @Column(length = 150)
    private String asunto;

    @Column(columnDefinition = "text")
    private String detalle;

    @Column(name = "ocurrido_en", nullable = false)
    private OffsetDateTime ocurridoEn;

    @Column(name = "seguimiento_en")
    private LocalDate seguimientoEn;

    @Column(name = "seguimiento_notificado_en")
    private OffsetDateTime seguimientoNotificadoEn;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    protected Interaccion() {
    }

    public static Interaccion registrar(UUID negocioId, UUID clienteId, UUID usuarioId,
            TipoInteraccion tipo, String asunto, String detalle, OffsetDateTime ocurridoEn,
            LocalDate seguimientoEn) {
        Interaccion i = new Interaccion();
        i.id = UUID.randomUUID();
        i.negocioId = negocioId;
        i.clienteId = clienteId;
        i.usuarioId = usuarioId;
        i.tipo = tipo;
        i.asunto = asunto;
        i.detalle = detalle;
        i.ocurridoEn = ocurridoEn == null ? OffsetDateTime.now() : ocurridoEn;
        i.seguimientoEn = seguimientoEn;
        return i;
    }

    /** El barrido ya avisó al responsable: no volver a hacerlo. */
    public void marcarSeguimientoNotificado() {
        this.seguimientoNotificadoEn = OffsetDateTime.now();
    }

    public boolean tieneSeguimientoPendiente() {
        return seguimientoEn != null && seguimientoNotificadoEn == null;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getClienteId() {
        return clienteId;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public TipoInteraccion getTipo() {
        return tipo;
    }

    public String getAsunto() {
        return asunto;
    }

    public String getDetalle() {
        return detalle;
    }

    public OffsetDateTime getOcurridoEn() {
        return ocurridoEn;
    }

    public LocalDate getSeguimientoEn() {
        return seguimientoEn;
    }

    public OffsetDateTime getSeguimientoNotificadoEn() {
        return seguimientoNotificadoEn;
    }
}
