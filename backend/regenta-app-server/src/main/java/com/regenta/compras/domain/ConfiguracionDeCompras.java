package com.regenta.compras.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.UpdateTimestamp;

import com.regenta.shared.web.ReglaDeNegocioException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * HU-173 · Cómo compra un negocio (I9, I15, I33 de {@code docs/dominio/compras.md}): si sus órdenes
 * necesitan aprobación —siempre, o solo desde un monto— y cuánto acepta recibir de más sobre lo
 * pedido. Sin fila guardada valen los valores por omisión de {@link #porOmision}.
 */
@Entity
@Table(name = "configuracion", schema = "compras")
public class ConfiguracionDeCompras {

    public static final BigDecimal TOLERANCIA_POR_OMISION = new BigDecimal("5");
    private static final BigDecimal CIEN = new BigDecimal("100");

    @Id
    @Column(name = "negocio_id")
    private UUID negocioId;

    @Column(name = "exige_aprobacion", nullable = false)
    private boolean exigeAprobacion;

    /** Desde qué total una orden necesita aprobación; nulo: todas la necesitan. */
    @Column(name = "monto_minimo_aprobacion")
    private BigDecimal montoMinimoAprobacion;

    @Column(name = "tolerancia_recepcion_pct", nullable = false)
    private BigDecimal toleranciaRecepcionPct;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    @Version
    @Column(nullable = false)
    private long version;

    protected ConfiguracionDeCompras() {
    }

    /** C1 · Un negocio nuevo: exige aprobación, sin monto mínimo, tolerancia 5 %. */
    public static ConfiguracionDeCompras porOmision(UUID negocioId) {
        ConfiguracionDeCompras c = new ConfiguracionDeCompras();
        c.negocioId = negocioId;
        c.exigeAprobacion = true;
        c.montoMinimoAprobacion = null;
        c.toleranciaRecepcionPct = TOLERANCIA_POR_OMISION;
        return c;
    }

    public void cambiar(boolean exigeAprobacion, BigDecimal montoMinimo, BigDecimal toleranciaPct) {
        if (montoMinimo != null && montoMinimo.signum() <= 0) {
            throw new ReglaDeNegocioException("El monto mínimo de aprobación debe ser mayor que cero, o vacío");
        }
        BigDecimal tolerancia = toleranciaPct == null ? TOLERANCIA_POR_OMISION : toleranciaPct;
        if (tolerancia.signum() < 0 || tolerancia.compareTo(CIEN) > 0) {
            throw new ReglaDeNegocioException("La tolerancia de recepción va de 0 a 100 %");
        }
        this.exigeAprobacion = exigeAprobacion;
        this.montoMinimoAprobacion = exigeAprobacion ? montoMinimo : null;
        this.toleranciaRecepcionPct = tolerancia;
    }

    /**
     * I9 · Si una orden por ese total necesita que alguien la apruebe. Sin exigencia, nunca; con monto
     * mínimo, solo desde ese monto.
     */
    public boolean requiereAprobacion(BigDecimal total) {
        if (!exigeAprobacion) {
            return false;
        }
        return montoMinimoAprobacion == null || total.compareTo(montoMinimoAprobacion) >= 0;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public boolean isExigeAprobacion() {
        return exigeAprobacion;
    }

    public BigDecimal getMontoMinimoAprobacion() {
        return montoMinimoAprobacion;
    }

    public BigDecimal getToleranciaRecepcionPct() {
        return toleranciaRecepcionPct;
    }
}
