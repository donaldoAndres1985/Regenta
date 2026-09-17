package com.regenta.comandas.domain;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import com.regenta.comun.errores.ReglaDeNegocioException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Un ticket de cocina (HU-088): agrupa, por estación, las líneas que se
 * enviaron juntas. Avanza aparte del ciclo de cada {@link ComandaLinea}: al
 * llegar a {@code LISTO} es lo que dispara el aviso al mesero.
 */
@Entity
@Table(name = "tickets_cocina")
public class TicketCocina {

    /** Umbral por defecto para marcarse demorado (HU-088 criterio 3). */
    private static final int DEMORA_ALERTA_MIN_DEFECTO = 15;

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "sucursal_id")
    private UUID sucursalId;

    @Column(name = "comanda_id", nullable = false, updatable = false)
    private UUID comandaId;

    @Column(name = "estacion_id", nullable = false, updatable = false)
    private UUID estacionId;

    @Column(nullable = false)
    private int secuencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoDeTicket estado;

    @Column(nullable = false)
    private short prioridad;

    @Column(name = "tiempo_objetivo_min")
    private Short tiempoObjetivoMin;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "iniciado_en")
    private OffsetDateTime iniciadoEn;

    @Column(name = "listo_en")
    private OffsetDateTime listoEn;

    @Column(name = "entregado_en")
    private OffsetDateTime entregadoEn;

    @Column(name = "usuario_cocina_id")
    private UUID usuarioCocinaId;

    protected TicketCocina() {
    }

    public static TicketCocina crear(UUID negocioId, UUID comandaId, UUID estacionId,
            int secuencia) {
        TicketCocina t = new TicketCocina();
        t.id = UUID.randomUUID();
        t.negocioId = negocioId;
        t.comandaId = comandaId;
        t.estacionId = estacionId;
        t.secuencia = secuencia;
        t.estado = EstadoDeTicket.NUEVO;
        t.prioridad = 0;
        t.creadoEn = OffsetDateTime.now();
        return t;
    }

    /** Fija cuántos minutos tolera este ticket antes de marcarse demorado. */
    public void fijarTiempoObjetivo(Short minutos) {
        this.tiempoObjetivoMin = minutos;
    }

    /**
     * Avanza al siguiente estado (HU-088 criterio 4): NUEVO → EN_PREPARACION →
     * LISTO → ENTREGADO, dejando su marca de tiempo y quién lo hizo.
     */
    public void avanzar(UUID usuarioCocinaId) {
        if (estado == EstadoDeTicket.ANULADO) {
            throw new ReglaDeNegocioException("Un ticket anulado no avanza");
        }
        EstadoDeTicket siguiente = estado.siguiente();
        this.estado = siguiente;
        this.usuarioCocinaId = usuarioCocinaId;
        OffsetDateTime ahora = OffsetDateTime.now();
        switch (siguiente) {
            case EN_PREPARACION -> this.iniciadoEn = ahora;
            case LISTO -> this.listoEn = ahora;
            case ENTREGADO -> this.entregadoEn = ahora;
            default -> {
                // NUEVO no tiene columna propia de tiempo.
            }
        }
    }

    /**
     * Un ticket vivo con más de {@code tiempoObjetivoMin} (o 15 minutos por
     * defecto) desde que se creó se marca demorado (HU-088 criterio 3). Un
     * ticket ya entregado o anulado deja de contar: ya no espera en cocina.
     */
    public boolean demorado(OffsetDateTime ahora) {
        if (estado == EstadoDeTicket.ENTREGADO || estado == EstadoDeTicket.ANULADO) {
            return false;
        }
        int limite = tiempoObjetivoMin != null ? tiempoObjetivoMin : DEMORA_ALERTA_MIN_DEFECTO;
        return ChronoUnit.MINUTES.between(creadoEn, ahora) > limite;
    }

    public int minutosTranscurridos(OffsetDateTime ahora) {
        return (int) Math.max(0, ChronoUnit.MINUTES.between(creadoEn, ahora));
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getSucursalId() {
        return sucursalId;
    }

    public UUID getComandaId() {
        return comandaId;
    }

    public UUID getEstacionId() {
        return estacionId;
    }

    public int getSecuencia() {
        return secuencia;
    }

    public EstadoDeTicket getEstado() {
        return estado;
    }

    public short getPrioridad() {
        return prioridad;
    }

    public Short getTiempoObjetivoMin() {
        return tiempoObjetivoMin;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }

    public OffsetDateTime getIniciadoEn() {
        return iniciadoEn;
    }

    public OffsetDateTime getListoEn() {
        return listoEn;
    }

    public OffsetDateTime getEntregadoEn() {
        return entregadoEn;
    }

    public UUID getUsuarioCocinaId() {
        return usuarioCocinaId;
    }
}
