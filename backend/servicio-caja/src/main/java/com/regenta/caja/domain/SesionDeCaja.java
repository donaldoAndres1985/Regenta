package com.regenta.caja.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import com.regenta.comun.errores.ConflictoDeEstadoException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Un turno de caja: se abre con una base y se cierra declarando lo contado
 * (HU-059). {@code diferencia} la calcula la base (columna generada), no la
 * app; el estado al cerrar es {@code CUADRADA} o {@code DESCUADRADA} según esa
 * diferencia.
 */
@Entity
@Table(name = "sesiones_caja")
public class SesionDeCaja {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "sucursal_id")
    private UUID sucursalId;

    @Column(name = "caja_id", nullable = false, updatable = false)
    private UUID cajaId;

    @Column(nullable = false, length = 30, updatable = false)
    private String numero;

    @Column(name = "usuario_apertura_id", nullable = false, updatable = false)
    private UUID usuarioAperturaId;

    @CreationTimestamp
    @Column(name = "abierta_en", nullable = false, updatable = false)
    private OffsetDateTime abiertaEn;

    @Column(name = "monto_apertura", nullable = false, updatable = false)
    private BigDecimal montoApertura;

    @Column(name = "usuario_cierre_id")
    private UUID usuarioCierreId;

    @Column(name = "cerrada_en")
    private OffsetDateTime cerradaEn;

    @Column(name = "monto_esperado")
    private BigDecimal montoEsperado;

    @Column(name = "monto_declarado")
    private BigDecimal montoDeclarado;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(insertable = false, updatable = false)
    private BigDecimal diferencia;

    @Column(name = "total_efectivo", nullable = false)
    private BigDecimal totalEfectivo;

    @Column(name = "total_tarjetas", nullable = false)
    private BigDecimal totalTarjetas;

    @Column(name = "total_transferencias", nullable = false)
    private BigDecimal totalTransferencias;

    @Column(name = "total_otros", nullable = false)
    private BigDecimal totalOtros;

    @Column(name = "total_propinas", nullable = false)
    private BigDecimal totalPropinas;

    @Column(name = "total_ingresos", nullable = false)
    private BigDecimal totalIngresos;

    @Column(name = "total_retiros", nullable = false)
    private BigDecimal totalRetiros;

    @Column(name = "num_transacciones", nullable = false)
    private int numTransacciones;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSesionCaja estado;

    @Column(columnDefinition = "text")
    private String observaciones;

    @Version
    @Column(nullable = false)
    private long version;

    protected SesionDeCaja() {
    }

    public static SesionDeCaja abrir(UUID negocioId, UUID sucursalId, UUID cajaId, String numero,
            UUID usuarioId, BigDecimal montoApertura) {
        SesionDeCaja s = new SesionDeCaja();
        s.id = UUID.randomUUID();
        s.negocioId = negocioId;
        s.sucursalId = sucursalId;
        s.cajaId = cajaId;
        s.numero = numero;
        s.usuarioAperturaId = usuarioId;
        s.montoApertura = montoApertura == null || montoApertura.signum() < 0
                ? BigDecimal.ZERO : montoApertura;
        s.estado = EstadoSesionCaja.ABIERTA;
        s.totalEfectivo = BigDecimal.ZERO;
        s.totalTarjetas = BigDecimal.ZERO;
        s.totalTransferencias = BigDecimal.ZERO;
        s.totalOtros = BigDecimal.ZERO;
        s.totalPropinas = BigDecimal.ZERO;
        s.totalIngresos = BigDecimal.ZERO;
        s.totalRetiros = BigDecimal.ZERO;
        s.numTransacciones = 0;
        return s;
    }

    /**
     * Criterio 3 y 4: cierra con lo esperado (calculado) y lo declarado
     * (contado). El estado sale de comparar los dos —igual que la columna
     * generada {@code diferencia}—.
     */
    public void cerrar(UUID usuarioId, BigDecimal montoEsperado, BigDecimal montoDeclarado,
            BigDecimal totalEfectivo, String observaciones) {
        if (estado != EstadoSesionCaja.ABIERTA) {
            throw new ConflictoDeEstadoException("La sesión de caja ya está cerrada");
        }
        this.usuarioCierreId = usuarioId;
        this.cerradaEn = OffsetDateTime.now();
        this.montoEsperado = montoEsperado;
        this.montoDeclarado = montoDeclarado;
        this.totalEfectivo = totalEfectivo;
        this.observaciones = observaciones;
        this.estado = montoDeclarado.compareTo(montoEsperado) == 0
                ? EstadoSesionCaja.CUADRADA : EstadoSesionCaja.DESCUADRADA;
    }

    public boolean quedoDescuadrada() {
        return estado == EstadoSesionCaja.DESCUADRADA;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getCajaId() {
        return cajaId;
    }

    public String getNumero() {
        return numero;
    }

    public UUID getUsuarioAperturaId() {
        return usuarioAperturaId;
    }

    public BigDecimal getMontoApertura() {
        return montoApertura;
    }

    public BigDecimal getMontoEsperado() {
        return montoEsperado;
    }

    public BigDecimal getMontoDeclarado() {
        return montoDeclarado;
    }

    public BigDecimal getDiferencia() {
        return diferencia;
    }

    public EstadoSesionCaja getEstado() {
        return estado;
    }

    public OffsetDateTime getAbiertaEn() {
        return abiertaEn;
    }

    public OffsetDateTime getCerradaEn() {
        return cerradaEn;
    }

    public String getObservaciones() {
        return observaciones;
    }
}
