package com.regenta.alertas.domain;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Una alerta encaminada a un usuario por un canal (HU-092 la crea
 * {@code PENDIENTE}; HU-094 la envía, la reintenta con backoff si falla y la
 * retiene si cae en "no molestar").
 */
@Entity
@Table(name = "entregas")
public class Entrega {

    /** Tras este número de intentos fallidos, la entrega se da por perdida. */
    public static final int MAX_INTENTOS = 5;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEntrega estado;

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

    @Column(name = "proximo_intento")
    private OffsetDateTime proximoIntento;

    @Column(name = "retenida_hasta")
    private OffsetDateTime retenidaHasta;

    protected Entrega() {
    }

    public static Entrega pendiente(UUID negocioId, UUID alertaId, UUID usuarioId,
            CanalDeAlerta canal, String destino) {
        Entrega e = new Entrega();
        e.id = UUID.randomUUID();
        e.negocioId = negocioId;
        e.alertaId = alertaId;
        e.usuarioId = usuarioId;
        e.canal = canal;
        e.destino = destino;
        e.estado = EstadoEntrega.PENDIENTE;
        e.intentos = 0;
        return e;
    }

    /** Criterio 4: se retiene hasta que termine la franja de "no molestar". */
    public void retenerHasta(OffsetDateTime fin) {
        this.estado = EstadoEntrega.PENDIENTE;
        this.retenidaHasta = fin;
        this.proximoIntento = fin;
    }

    public void marcarEnviada(String proveedorId) {
        this.estado = EstadoEntrega.ENVIADA;
        this.proveedorId = proveedorId;
        this.enviadaEn = OffsetDateTime.now();
        this.error = null;
        this.proximoIntento = null;
        this.retenidaHasta = null;
        this.intentos++;
    }

    /** Canal in-app: no hay envío externo, la alerta ya está en la base. */
    public void marcarEntregada() {
        this.estado = EstadoEntrega.ENTREGADA;
        this.enviadaEn = OffsetDateTime.now();
        this.proximoIntento = null;
        this.retenidaHasta = null;
        this.intentos++;
    }

    /** Criterio 3: cuenta el intento, guarda el error y agenda el siguiente con backoff. */
    public void marcarFallida(String error) {
        this.intentos++;
        this.error = error;
        if (intentos >= MAX_INTENTOS) {
            this.estado = EstadoEntrega.FALLIDA;
            this.proximoIntento = null;
        } else {
            this.estado = EstadoEntrega.FALLIDA;
            long minutos = (long) Math.pow(2, intentos); // 2, 4, 8, 16 min
            this.proximoIntento = OffsetDateTime.now().plus(Duration.ofMinutes(minutos));
        }
    }

    public void marcarLeida() {
        this.estado = EstadoEntrega.LEIDA;
        this.leidaEn = OffsetDateTime.now();
    }

    /** El usuario no quiere este canal para este tipo: queda registrada, no se envía. */
    public void descartarPorPreferencia() {
        this.estado = EstadoEntrega.FALLIDA;
        this.error = "El usuario no acepta este canal para este tipo de alerta";
        this.intentos = MAX_INTENTOS;
        this.proximoIntento = null;
    }

    public boolean estaPendienteDeEnvio() {
        return estado == EstadoEntrega.PENDIENTE
                || (estado == EstadoEntrega.FALLIDA && intentos < MAX_INTENTOS);
    }

    public boolean tocaReintentar(OffsetDateTime ahora) {
        return estaPendienteDeEnvio()
                && (proximoIntento == null || !proximoIntento.isAfter(ahora));
    }

    public boolean seDaPorPerdida() {
        return estado == EstadoEntrega.FALLIDA && intentos >= MAX_INTENTOS;
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

    public String getDestino() {
        return destino;
    }

    public EstadoEntrega getEstado() {
        return estado;
    }

    public String getProveedorId() {
        return proveedorId;
    }

    public int getIntentos() {
        return intentos;
    }

    public String getError() {
        return error;
    }

    public OffsetDateTime getProximoIntento() {
        return proximoIntento;
    }

    public OffsetDateTime getRetenidaHasta() {
        return retenidaHasta;
    }
}
