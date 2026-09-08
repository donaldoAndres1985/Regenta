package com.regenta.alertas.domain;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Lo que un usuario acepta de un tipo de alerta (HU-094): por qué canales, si lo
 * quiere, y su franja de "no molestar" (criterio 4). Sin fila, el usuario recibe
 * por los canales de la regla y sin franja de silencio.
 */
@Entity
@Table(name = "preferencias_notificacion")
@IdClass(PreferenciaNotificacionId.class)
public class PreferenciaNotificacion {

    @Id
    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Id
    @Column(name = "tipo_codigo", length = 40)
    private String tipoCodigo;

    @Column(name = "negocio_id", nullable = false)
    private UUID negocioId;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    private String[] canales;

    @Column(nullable = false)
    private boolean habilitada;

    @Column(name = "no_molestar_desde")
    private LocalTime noMolestarDesde;

    @Column(name = "no_molestar_hasta")
    private LocalTime noMolestarHasta;

    protected PreferenciaNotificacion() {
    }

    public static PreferenciaNotificacion de(UUID negocioId, UUID usuarioId, String tipoCodigo,
            List<String> canales, boolean habilitada, LocalTime noMolestarDesde,
            LocalTime noMolestarHasta) {
        PreferenciaNotificacion p = new PreferenciaNotificacion();
        p.negocioId = negocioId;
        p.usuarioId = usuarioId;
        p.tipoCodigo = tipoCodigo;
        p.aplicar(canales, habilitada, noMolestarDesde, noMolestarHasta);
        return p;
    }

    public void aplicar(List<String> canales, boolean habilitada, LocalTime noMolestarDesde,
            LocalTime noMolestarHasta) {
        this.canales = (canales == null || canales.isEmpty() ? List.of("IN_APP") : canales)
                .toArray(String[]::new);
        this.habilitada = habilitada;
        this.noMolestarDesde = noMolestarDesde;
        this.noMolestarHasta = noMolestarHasta;
    }

    public boolean aceptaCanal(String canal) {
        if (!habilitada) {
            return false;
        }
        for (String c : canales) {
            if (c.equals(canal)) {
                return true;
            }
        }
        return false;
    }

    /** Criterio 4: si la hora cae en la franja de "no molestar". Cruza medianoche. */
    public boolean enNoMolestar(LocalTime hora) {
        if (noMolestarDesde == null || noMolestarHasta == null) {
            return false;
        }
        if (noMolestarDesde.equals(noMolestarHasta)) {
            return false;
        }
        if (noMolestarDesde.isBefore(noMolestarHasta)) {
            return !hora.isBefore(noMolestarDesde) && hora.isBefore(noMolestarHasta);
        }
        // Franja que cruza medianoche: p.ej. 22:00 -> 07:00
        return !hora.isBefore(noMolestarDesde) || hora.isBefore(noMolestarHasta);
    }

    public String getTipoCodigo() {
        return tipoCodigo;
    }

    public LocalTime getNoMolestarDesde() {
        return noMolestarDesde;
    }

    public LocalTime getNoMolestarHasta() {
        return noMolestarHasta;
    }

    public List<String> getCanales() {
        return List.of(canales);
    }

    public boolean isHabilitada() {
        return habilitada;
    }
}
