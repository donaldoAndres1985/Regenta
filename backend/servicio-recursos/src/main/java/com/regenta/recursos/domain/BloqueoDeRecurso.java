package com.regenta.recursos.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.regenta.comun.errores.ReglaDeNegocioException;

/**
 * Un periodo en el que un recurso no se puede reservar (HU-067): la habitación
 * está en obra, la cancha tiene un evento, el spa cierra por feriado. Dos
 * bloqueos del mismo recurso no se pueden solapar — lo garantiza un
 * {@code EXCLUDE USING gist} en la tabla, no este código.
 *
 * <p>La columna {@code periodo} es un {@code tstzrange} de Postgres; en Java se
 * lleva como dos instantes {@code [desde, hasta)} y el repositorio arma y
 * desarma el rango. Por eso esta clase no es una entidad JPA.
 */
public final class BloqueoDeRecurso {

    private final UUID id;
    private final UUID negocioId;
    private final UUID recursoId;
    private final OffsetDateTime desde;
    private final OffsetDateTime hasta;
    private final MotivoBloqueo motivo;
    private final String detalle;
    private final UUID usuarioId;
    private final OffsetDateTime creadoEn;

    public BloqueoDeRecurso(UUID id, UUID negocioId, UUID recursoId, OffsetDateTime desde,
            OffsetDateTime hasta, MotivoBloqueo motivo, String detalle, UUID usuarioId,
            OffsetDateTime creadoEn) {
        this.id = id;
        this.negocioId = negocioId;
        this.recursoId = recursoId;
        this.desde = desde;
        this.hasta = hasta;
        this.motivo = motivo;
        this.detalle = detalle;
        this.usuarioId = usuarioId;
        this.creadoEn = creadoEn;
    }

    /** Un bloqueo nuevo, sin persistir todavía. Valida que el periodo tenga sentido. */
    public static BloqueoDeRecurso nuevo(UUID negocioId, UUID recursoId, OffsetDateTime desde,
            OffsetDateTime hasta, MotivoBloqueo motivo, String detalle, UUID usuarioId) {
        if (desde == null || hasta == null) {
            throw new ReglaDeNegocioException("El bloqueo necesita fecha de inicio y de fin");
        }
        if (!hasta.isAfter(desde)) {
            throw new ReglaDeNegocioException("El fin del bloqueo debe ser posterior al inicio");
        }
        return new BloqueoDeRecurso(UUID.randomUUID(), negocioId, recursoId, desde, hasta,
                motivo == null ? MotivoBloqueo.OTRO : motivo,
                detalle == null || detalle.isBlank() ? null : detalle.trim(), usuarioId, null);
    }

    /** {@code [desde, hasta)} se solapa con {@code [otroDesde, otroHasta)}. */
    public boolean solapaCon(OffsetDateTime otroDesde, OffsetDateTime otroHasta) {
        return desde.isBefore(otroHasta) && hasta.isAfter(otroDesde);
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getRecursoId() {
        return recursoId;
    }

    public OffsetDateTime getDesde() {
        return desde;
    }

    public OffsetDateTime getHasta() {
        return hasta;
    }

    public MotivoBloqueo getMotivo() {
        return motivo;
    }

    public String getDetalle() {
        return detalle;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }
}
