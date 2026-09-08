package com.regenta.caja.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Configuración de caja del negocio (HU-061). Por ahora solo el umbral de retiro
 * que exige autorización; {@code 0} = sin límite.
 */
@Entity
@Table(name = "config_caja")
public class ConfigDeCaja {

    @Id
    @Column(name = "negocio_id")
    private UUID negocioId;

    @Column(name = "retiro_max_sin_autorizacion", nullable = false)
    private BigDecimal retiroMaxSinAutorizacion;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    @Version
    @Column(nullable = false)
    private long version;

    protected ConfigDeCaja() {
    }

    public static ConfigDeCaja porDefecto(UUID negocioId) {
        ConfigDeCaja c = new ConfigDeCaja();
        c.negocioId = negocioId;
        c.retiroMaxSinAutorizacion = BigDecimal.ZERO;
        return c;
    }

    public void fijarUmbral(BigDecimal umbral) {
        this.retiroMaxSinAutorizacion = umbral == null || umbral.signum() < 0
                ? BigDecimal.ZERO : umbral;
    }

    /** El retiro necesita autorización si hay umbral (> 0) y el monto lo supera. */
    public boolean exigeAutorizacion(BigDecimal montoRetiro) {
        return retiroMaxSinAutorizacion.signum() > 0
                && montoRetiro.compareTo(retiroMaxSinAutorizacion) > 0;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public BigDecimal getRetiroMaxSinAutorizacion() {
        return retiroMaxSinAutorizacion;
    }
}
