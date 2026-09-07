package com.regenta.facturacion.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.regenta.comun.errores.ReglaDeNegocioException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Una resolución de numeración de la DIAN: el rango de números que el negocio
 * tiene autorizado a facturar, con su clave técnica y su vigencia (HU-052).
 *
 * <p>El consecutivo NO es un {@code BIGSERIAL}: es {@code consecutivoActual} y se
 * toma con bloqueo dentro de la transacción de emisión (HU-054). Cuando llega a
 * {@code rangoHasta + 1} la resolución queda {@link EstadoResolucion#AGOTADA} —el
 * CHECK {@code ck_consecutivo} de la base lo permite justo hasta ahí.
 */
@Entity
@Table(name = "resoluciones")
public class Resolucion {

    /** Fracción del rango por debajo de la cual conviene avisar (criterio 4). */
    public static final double UMBRAL_DE_AVISO = 0.10;

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "sucursal_id")
    private UUID sucursalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 20)
    private TipoDocumento tipoDocumento;

    @Column(name = "numero_resolucion", nullable = false, length = 40)
    private String numeroResolucion;

    @Column(nullable = false, length = 10)
    private String prefijo;

    @Column(name = "rango_desde", nullable = false)
    private long rangoDesde;

    @Column(name = "rango_hasta", nullable = false)
    private long rangoHasta;

    @Column(name = "consecutivo_actual", nullable = false)
    private long consecutivoActual;

    @Column(name = "clave_tecnica", length = 120)
    private String claveTecnica;

    @Column(name = "vigente_desde", nullable = false)
    private LocalDate vigenteDesde;

    @Column(name = "vigente_hasta", nullable = false)
    private LocalDate vigenteHasta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Ambiente ambiente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoResolucion estado;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Version
    @Column(nullable = false)
    private long version;

    protected Resolucion() {
    }

    public static Resolucion cargar(UUID negocioId, UUID sucursalId, TipoDocumento tipoDocumento,
            String numeroResolucion, String prefijo, long rangoDesde, long rangoHasta,
            String claveTecnica, LocalDate vigenteDesde, LocalDate vigenteHasta,
            Ambiente ambiente) {
        if (rangoDesde < 1) {
            throw new ReglaDeNegocioException("El rango debe empezar en 1 o más");
        }
        if (rangoHasta < rangoDesde) {
            throw new ReglaDeNegocioException(
                    "El fin del rango (" + rangoHasta + ") no puede ser menor que el inicio ("
                            + rangoDesde + ")");
        }
        if (vigenteDesde == null || vigenteHasta == null || vigenteHasta.isBefore(vigenteDesde)) {
            throw new ReglaDeNegocioException(
                    "La vigencia termina antes de empezar");
        }
        Resolucion r = new Resolucion();
        r.id = UUID.randomUUID();
        r.negocioId = negocioId;
        r.sucursalId = sucursalId;
        r.tipoDocumento = tipoDocumento;
        r.numeroResolucion = numeroResolucion;
        r.prefijo = prefijo == null ? "" : prefijo;
        r.rangoDesde = rangoDesde;
        r.rangoHasta = rangoHasta;
        r.consecutivoActual = rangoDesde;
        r.claveTecnica = claveTecnica;
        r.vigenteDesde = vigenteDesde;
        r.vigenteHasta = vigenteHasta;
        r.ambiente = ambiente == null ? Ambiente.PRUEBAS : ambiente;
        r.estado = EstadoResolucion.VIGENTE;
        return r;
    }

    /** Cuántos números quedan por usar (0 si está agotada). */
    public long numerosDisponibles() {
        return Math.max(0, rangoHasta - consecutivoActual + 1);
    }

    public long totalDelRango() {
        return rangoHasta - rangoDesde + 1;
    }

    /** Criterio 4: queda menos de {@link #UMBRAL_DE_AVISO} del rango. */
    public boolean porDebajoDelUmbral() {
        return numerosDisponibles() < totalDelRango() * UMBRAL_DE_AVISO;
    }

    public boolean estaVigenteEn(LocalDate fecha) {
        return estado == EstadoResolucion.VIGENTE
                && !fecha.isBefore(vigenteDesde) && !fecha.isAfter(vigenteHasta);
    }

    /** Criterio 5: no se factura con una resolución vencida o no vigente. */
    public void exigirVigenteEn(LocalDate fecha) {
        if (estado != EstadoResolucion.VIGENTE) {
            throw new ReglaDeNegocioException(
                    "La resolución " + numeroResolucion + " está " + estado.name().toLowerCase());
        }
        if (fecha.isAfter(vigenteHasta)) {
            throw new ReglaDeNegocioException(
                    "La resolución " + numeroResolucion + " venció el " + vigenteHasta);
        }
        if (fecha.isBefore(vigenteDesde)) {
            throw new ReglaDeNegocioException(
                    "La resolución " + numeroResolucion + " entra en vigencia el " + vigenteDesde);
        }
    }

    public void anular() {
        this.estado = EstadoResolucion.ANULADA;
    }

    public void marcarVencida() {
        this.estado = EstadoResolucion.VENCIDA;
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

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public String getNumeroResolucion() {
        return numeroResolucion;
    }

    public String getPrefijo() {
        return prefijo;
    }

    public long getRangoDesde() {
        return rangoDesde;
    }

    public long getRangoHasta() {
        return rangoHasta;
    }

    public long getConsecutivoActual() {
        return consecutivoActual;
    }

    public String getClaveTecnica() {
        return claveTecnica;
    }

    public LocalDate getVigenteDesde() {
        return vigenteDesde;
    }

    public LocalDate getVigenteHasta() {
        return vigenteHasta;
    }

    public Ambiente getAmbiente() {
        return ambiente;
    }

    public EstadoResolucion getEstado() {
        return estado;
    }
}
