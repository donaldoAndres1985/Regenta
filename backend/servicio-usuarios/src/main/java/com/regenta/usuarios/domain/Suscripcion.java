package com.regenta.usuarios.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Historial de suscripcion, no un campo en el negocio.
 *
 * <p>Guardarlo como historial es lo que permite facturar el SaaS, auditar
 * cambios de plan y responder "que modulos tenia activos este negocio el 12 de
 * marzo". Solo una puede estar vigente por negocio: lo garantiza un indice
 * unico parcial sobre {@code fecha_fin IS NULL}.
 */
@Entity
@Table(name = "suscripciones")
public class Suscripcion {

    public static final String VIGENTE = "VIGENTE";
    public static final String VENCIDA = "VENCIDA";
    public static final String CANCELADA = "CANCELADA";

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(nullable = false, length = 20)
    private String periodicidad;

    @Column(name = "precio_pactado", nullable = false)
    private BigDecimal precioPactado;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private String moneda;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "motivo_cambio")
    private String motivoCambio;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    protected Suscripcion() {
    }

    public static Suscripcion vigente(UUID negocioId, UUID planId, LocalDate desde,
            String periodicidad, BigDecimal precio, String moneda) {
        Suscripcion suscripcion = new Suscripcion();
        suscripcion.id = UUID.randomUUID();
        suscripcion.negocioId = negocioId;
        suscripcion.planId = planId;
        suscripcion.fechaInicio = desde;
        suscripcion.periodicidad = periodicidad;
        suscripcion.precioPactado = precio;
        suscripcion.moneda = moneda;
        suscripcion.estado = VIGENTE;
        return suscripcion;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getPlanId() {
        return planId;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public String getEstado() {
        return estado;
    }

    public BigDecimal getPrecioPactado() {
        return precioPactado;
    }

    public boolean estaVigente() {
        return fechaFin == null;
    }

    /** Se cierra la vigente antes de abrir la nueva: el indice unico no admite dos. */
    public void cerrar(LocalDate hasta, String motivo) {
        this.fechaFin = hasta;
        this.estado = VENCIDA;
        this.motivoCambio = motivo;
    }
}
