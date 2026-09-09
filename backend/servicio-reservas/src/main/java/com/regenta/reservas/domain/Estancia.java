package com.regenta.reservas.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * El alojamiento de un huésped entre el check-in y el check-out (HU-072). Se
 * abre al hacer check-in y queda ligada 1:1 a la reserva
 * ({@code estancias.reserva_id UNIQUE}).
 */
public final class Estancia {

    private final UUID id;
    private final UUID negocioId;
    private final UUID reservaId;
    private final UUID recursoAsignadoId;
    private final OffsetDateTime checkInEn;
    private final UUID checkInUsuarioId;
    private final OffsetDateTime checkOutPrevisto;
    private final EstadoEstancia estado;
    private final BigDecimal consumoTotal;
    private final BigDecimal deposito;
    private final String observacionesEntrada;
    private final long version;

    private Estancia(UUID id, UUID negocioId, UUID reservaId, UUID recursoAsignadoId,
            OffsetDateTime checkInEn, UUID checkInUsuarioId, OffsetDateTime checkOutPrevisto,
            EstadoEstancia estado, BigDecimal consumoTotal, BigDecimal deposito,
            String observacionesEntrada, long version) {
        this.id = id;
        this.negocioId = negocioId;
        this.reservaId = reservaId;
        this.recursoAsignadoId = recursoAsignadoId;
        this.checkInEn = checkInEn;
        this.checkInUsuarioId = checkInUsuarioId;
        this.checkOutPrevisto = checkOutPrevisto;
        this.estado = estado;
        this.consumoTotal = consumoTotal;
        this.deposito = deposito;
        this.observacionesEntrada = observacionesEntrada;
        this.version = version;
    }

    /** Abre la estancia al hacer check-in. */
    public static Estancia abrir(UUID negocioId, UUID reservaId, UUID recursoAsignadoId,
            OffsetDateTime checkInEn, UUID checkInUsuarioId, OffsetDateTime checkOutPrevisto,
            BigDecimal deposito, String observacionesEntrada) {
        BigDecimal dep = deposito == null || deposito.signum() < 0 ? BigDecimal.ZERO : deposito;
        return new Estancia(UUID.randomUUID(), negocioId, reservaId, recursoAsignadoId, checkInEn,
                checkInUsuarioId, checkOutPrevisto, EstadoEstancia.EN_CURSO, BigDecimal.ZERO,
                dep.setScale(4, java.math.RoundingMode.HALF_UP),
                observacionesEntrada == null || observacionesEntrada.isBlank() ? null
                        : observacionesEntrada.trim(), 0L);
    }

    public static Estancia rehidratar(UUID id, UUID negocioId, UUID reservaId,
            UUID recursoAsignadoId, OffsetDateTime checkInEn, UUID checkInUsuarioId,
            OffsetDateTime checkOutPrevisto, EstadoEstancia estado, BigDecimal consumoTotal,
            BigDecimal deposito, String observacionesEntrada, long version) {
        return new Estancia(id, negocioId, reservaId, recursoAsignadoId, checkInEn, checkInUsuarioId,
                checkOutPrevisto, estado, consumoTotal, deposito, observacionesEntrada, version);
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getReservaId() {
        return reservaId;
    }

    public UUID getRecursoAsignadoId() {
        return recursoAsignadoId;
    }

    public OffsetDateTime getCheckInEn() {
        return checkInEn;
    }

    public UUID getCheckInUsuarioId() {
        return checkInUsuarioId;
    }

    public OffsetDateTime getCheckOutPrevisto() {
        return checkOutPrevisto;
    }

    public EstadoEstancia getEstado() {
        return estado;
    }

    public BigDecimal getConsumoTotal() {
        return consumoTotal;
    }

    public BigDecimal getDeposito() {
        return deposito;
    }

    public String getObservacionesEntrada() {
        return observacionesEntrada;
    }

    public long getVersion() {
        return version;
    }
}
