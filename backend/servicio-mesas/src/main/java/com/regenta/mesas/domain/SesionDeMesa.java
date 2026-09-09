package com.regenta.mesas.domain;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import com.regenta.comun.errores.ReglaDeNegocioException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Una ocupación de la mesa (HU-082). Se abre con el número de comensales y
 * arranca el cronómetro (criterio 1); solo una viva por mesa a la vez
 * (criterio 2, respaldado por {@code uq_sesion_abierta}); al cerrar la comanda
 * la mesa queda «por limpiar», no libre (criterio 3); y una vez cerrada se sabe
 * cuánto duró y cuántos comensales tuvo (criterio 5).
 */
@Entity
@Table(name = "sesiones_mesa")
public class SesionDeMesa {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "sucursal_id")
    private UUID sucursalId;

    @Column(name = "mesa_principal_id", nullable = false, updatable = false)
    private UUID mesaPrincipalId;

    @Column(name = "comanda_id")
    private UUID comandaId;

    @Column(name = "mesero_usuario_id")
    private UUID meseroUsuarioId;

    @Column(name = "num_comensales", nullable = false)
    private short numComensales;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoDeSesion estado;

    @Column(name = "abierta_en", nullable = false, updatable = false)
    private OffsetDateTime abiertaEn;

    @Column(name = "cerrada_en")
    private OffsetDateTime cerradaEn;

    /** La calcula la base (columna generada), no el código. */
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "duracion_min", insertable = false, updatable = false)
    private Integer duracionMin;

    @Version
    @Column(nullable = false)
    private long version;

    protected SesionDeMesa() {
    }

    public static SesionDeMesa abrir(UUID negocioId, UUID mesaPrincipalId, Integer numComensales,
            UUID meseroUsuarioId) {
        SesionDeMesa s = new SesionDeMesa();
        s.id = UUID.randomUUID();
        s.negocioId = negocioId;
        s.mesaPrincipalId = mesaPrincipalId;
        s.meseroUsuarioId = meseroUsuarioId;
        s.numComensales = comensalesValidos(numComensales);
        s.estado = EstadoDeSesion.ABIERTA;
        s.abiertaEn = OffsetDateTime.now();
        return s;
    }

    public void asignarComanda(UUID comandaId) {
        this.comandaId = comandaId;
    }

    public void ajustarComensales(Integer numComensales) {
        this.numComensales = comensalesValidos(numComensales);
    }

    public void pedirCuenta() {
        if (estado != EstadoDeSesion.ABIERTA) {
            throw new ReglaDeNegocioException("La sesión no está abierta");
        }
        this.estado = EstadoDeSesion.CUENTA_PEDIDA;
    }

    /** Cierra la sesión y fija su fin (HU-082 criterios 3 y 5). Idempotente. */
    public void cerrar(OffsetDateTime cuando) {
        if (estado == EstadoDeSesion.CERRADA || estado == EstadoDeSesion.ANULADA) {
            return;
        }
        this.estado = EstadoDeSesion.CERRADA;
        this.cerradaEn = cuando == null ? OffsetDateTime.now() : cuando;
    }

    public void anular() {
        if (estado == EstadoDeSesion.CERRADA) {
            throw new ReglaDeNegocioException("La sesión ya se cerró");
        }
        this.estado = EstadoDeSesion.ANULADA;
        this.cerradaEn = OffsetDateTime.now();
    }

    public boolean estaViva() {
        return estado.estaViva();
    }

    /**
     * Cuántos minutos lleva (o llevó) abierta. Sirve para el cronómetro en curso
     * y para el resumen de la sesión cerrada (criterio 5). No usa la columna
     * generada, que solo tiene valor tras el commit.
     */
    public long minutosTranscurridos() {
        OffsetDateTime fin = cerradaEn == null ? OffsetDateTime.now() : cerradaEn;
        return Math.max(0, ChronoUnit.MINUTES.between(abiertaEn, fin));
    }

    private static short comensalesValidos(Integer n) {
        if (n == null || n < 1) {
            throw new ReglaDeNegocioException("La sesión necesita al menos un comensal");
        }
        return n.shortValue();
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getMesaPrincipalId() {
        return mesaPrincipalId;
    }

    public UUID getComandaId() {
        return comandaId;
    }

    public UUID getMeseroUsuarioId() {
        return meseroUsuarioId;
    }

    public short getNumComensales() {
        return numComensales;
    }

    public EstadoDeSesion getEstado() {
        return estado;
    }

    public OffsetDateTime getAbiertaEn() {
        return abiertaEn;
    }

    public OffsetDateTime getCerradaEn() {
        return cerradaEn;
    }

    public Integer getDuracionMin() {
        return duracionMin;
    }

    public long getVersion() {
        return version;
    }
}
