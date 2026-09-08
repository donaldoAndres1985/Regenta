package com.regenta.recursos.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.regenta.comun.errores.ReglaDeNegocioException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Las condiciones de cancelación de una reserva (HU-068 criterio 3): con cuánta
 * antelación se puede cancelar sin costo, qué fracción se retiene si no, y qué
 * anticipo hay que exigir al reservar.
 *
 * <p>Los dos porcentajes se guardan como fracción: {@code 0.2500} es 25 %.
 */
@Entity
@Table(name = "politicas_cancelacion")
public class PoliticaCancelacion {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(name = "horas_antes", nullable = false)
    private int horasAntes;

    @Column(name = "penalizacion_pct", nullable = false)
    private BigDecimal penalizacionPct;

    @Column(name = "anticipo_requerido_pct", nullable = false)
    private BigDecimal anticipoRequeridoPct;

    @Column(name = "es_default", nullable = false)
    private boolean esDefault;

    @Column(nullable = false)
    private boolean activa;

    protected PoliticaCancelacion() {
    }

    public static PoliticaCancelacion crear(UUID negocioId, String nombre, int horasAntes,
            BigDecimal penalizacionPct, BigDecimal anticipoRequeridoPct, boolean esDefault) {
        PoliticaCancelacion p = new PoliticaCancelacion();
        p.id = UUID.randomUUID();
        p.negocioId = negocioId;
        p.activa = true;
        p.aplicar(nombre, horasAntes, penalizacionPct, anticipoRequeridoPct);
        p.esDefault = esDefault;
        return p;
    }

    public void editar(String nombre, int horasAntes, BigDecimal penalizacionPct,
            BigDecimal anticipoRequeridoPct) {
        aplicar(nombre, horasAntes, penalizacionPct, anticipoRequeridoPct);
    }

    private void aplicar(String nombre, int horasAntes, BigDecimal penalizacionPct,
            BigDecimal anticipoRequeridoPct) {
        if (horasAntes < 0) {
            throw new ReglaDeNegocioException("Las horas de antelación no pueden ser negativas");
        }
        this.nombre = nombre;
        this.horasAntes = horasAntes;
        this.penalizacionPct = fraccion(penalizacionPct, "penalización");
        this.anticipoRequeridoPct = fraccion(anticipoRequeridoPct, "anticipo requerido");
    }

    private static BigDecimal fraccion(BigDecimal valor, String campo) {
        BigDecimal v = valor == null ? BigDecimal.ZERO : valor;
        if (v.signum() < 0 || v.compareTo(BigDecimal.ONE) > 0) {
            throw new ReglaDeNegocioException(
                    "El porcentaje de " + campo + " va entre 0 y 1 (0.25 = 25 %)");
        }
        return v;
    }

    public void marcarPorDefecto() {
        this.esDefault = true;
    }

    public void quitarPorDefecto() {
        this.esDefault = false;
    }

    public void desactivar() {
        this.activa = false;
        this.esDefault = false;
    }

    /**
     * Aplica la política a una reserva que empieza en {@code entrada} y se
     * cancela en {@code ahora}. Si faltan {@code horasAntes} o más para la
     * entrada, no hay penalización.
     */
    public ResultadoDeCancelacion aplicarA(BigDecimal montoReserva, OffsetDateTime ahora,
            OffsetDateTime entrada) {
        BigDecimal monto = montoReserva == null || montoReserva.signum() < 0
                ? BigDecimal.ZERO : montoReserva;
        long horas = Duration.between(ahora, entrada).toHours();
        boolean dentroDePlazo = horas >= horasAntes;
        BigDecimal penalizacion = dentroDePlazo ? BigDecimal.ZERO
                : monto.multiply(penalizacionPct).setScale(4, RoundingMode.HALF_UP);
        BigDecimal anticipo = monto.multiply(anticipoRequeridoPct).setScale(4, RoundingMode.HALF_UP);
        return new ResultadoDeCancelacion(anticipo, penalizacion, horas, dentroDePlazo);
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public String getNombre() {
        return nombre;
    }

    public int getHorasAntes() {
        return horasAntes;
    }

    public BigDecimal getPenalizacionPct() {
        return penalizacionPct;
    }

    public BigDecimal getAnticipoRequeridoPct() {
        return anticipoRequeridoPct;
    }

    public boolean isEsDefault() {
        return esDefault;
    }

    public boolean isActiva() {
        return activa;
    }
}
