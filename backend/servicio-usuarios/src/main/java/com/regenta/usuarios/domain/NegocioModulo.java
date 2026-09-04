package com.regenta.usuarios.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Activacion efectiva de un modulo para un negocio.
 *
 * <p>Es lo que permite vender un add-on sin cambiar de plan, y apagar un modulo
 * puntual sin bajar de plan. Lo que manda no es el plan: es esta tabla.
 */
@Entity
@Table(name = "negocio_modulos")
public class NegocioModulo {

    public static final String POR_PLAN = "PLAN";
    public static final String ADDON = "ADDON";
    public static final String CORTESIA = "CORTESIA";

    @EmbeddedId
    private ClaveNegocioModulo clave;

    @Column(nullable = false)
    private boolean activo;

    @Column(nullable = false, length = 20)
    private String origen;

    @CreationTimestamp
    @Column(name = "activado_en", nullable = false, updatable = false)
    private OffsetDateTime activadoEn;

    @Column(name = "desactivado_en")
    private OffsetDateTime desactivadoEn;

    protected NegocioModulo() {
    }

    public static NegocioModulo activo(UUID negocioId, String moduloCodigo, String origen) {
        NegocioModulo modulo = new NegocioModulo();
        modulo.clave = new ClaveNegocioModulo(negocioId, moduloCodigo);
        modulo.activo = true;
        modulo.origen = origen;
        return modulo;
    }

    public ClaveNegocioModulo getClave() {
        return clave;
    }

    public String getModuloCodigo() {
        return clave.getModuloCodigo();
    }

    public UUID getNegocioId() {
        return clave.getNegocioId();
    }

    public boolean isActivo() {
        return activo;
    }

    public String getOrigen() {
        return origen;
    }

    public void desactivar(OffsetDateTime cuando) {
        this.activo = false;
        this.desactivadoEn = cuando;
    }

    public void reactivar() {
        this.activo = true;
        this.desactivadoEn = null;
    }
}
