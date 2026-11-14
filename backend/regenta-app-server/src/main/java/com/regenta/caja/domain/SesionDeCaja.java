package com.regenta.caja.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import com.regenta.shared.web.ConflictoDeEstadoException;

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
@Table(name = "sesiones_caja", schema = "caja")
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
     * HU-140: cierra la sesión con su arqueo por método de pago.
     *
     * <p>I11: queda {@code CUADRADA} solo si todas las líneas cuadran. I12: una sesión cerrada
     * no se vuelve a cerrar. {@code monto_esperado}/{@code monto_declarado} conservan la línea
     * de efectivo (HU-059), y los totales del turno salen de los esperados de cada método.
     */
    public void cerrarConArqueo(UUID usuarioId, List<LineaDeArqueo> lineas, String observaciones) {
        if (estado != EstadoSesionCaja.ABIERTA) {
            throw new ConflictoDeEstadoException("La sesión de caja ya está cerrada");
        }
        LineaDeArqueo efectivo = lineas.stream()
                .filter(l -> l.metodo() == MetodoPagoCaja.EFECTIVO).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El arqueo no trae línea de efectivo"));

        this.usuarioCierreId = usuarioId;
        this.cerradaEn = OffsetDateTime.now();
        this.montoEsperado = efectivo.esperado();
        this.montoDeclarado = efectivo.contado();
        this.totalEfectivo = efectivo.esperado();
        this.totalTarjetas = esperadoDe(lineas, MetodoPagoCaja.TARJETA_DEBITO)
                .add(esperadoDe(lineas, MetodoPagoCaja.TARJETA_CREDITO));
        this.totalTransferencias = esperadoDe(lineas, MetodoPagoCaja.TRANSFERENCIA);
        this.totalOtros = lineas.stream()
                .filter(l -> !EnumSet.of(MetodoPagoCaja.EFECTIVO, MetodoPagoCaja.TARJETA_DEBITO,
                        MetodoPagoCaja.TARJETA_CREDITO, MetodoPagoCaja.TRANSFERENCIA).contains(l.metodo()))
                .map(LineaDeArqueo::esperado).reduce(BigDecimal.ZERO, BigDecimal::add);
        this.numTransacciones = (int) lineas.stream().mapToLong(LineaDeArqueo::movimientos).sum();
        this.observaciones = observaciones;
        this.estado = lineas.stream().allMatch(LineaDeArqueo::cuadra)
                ? EstadoSesionCaja.CUADRADA : EstadoSesionCaja.DESCUADRADA;
    }

    private static BigDecimal esperadoDe(List<LineaDeArqueo> lineas, MetodoPagoCaja metodo) {
        return lineas.stream().filter(l -> l.metodo() == metodo).map(LineaDeArqueo::esperado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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
