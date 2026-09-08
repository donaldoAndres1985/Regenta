package com.regenta.caja.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Una entrada o salida de dinero de una sesión de caja (HU-059 registra la
 * apertura; HU-060 la enchufa a los cobros de los tres patrones). El
 * {@code idempotency_key} evita que el mismo cobro entre dos veces.
 */
@Entity
@Table(name = "movimientos_caja")
public class MovimientoDeCaja {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "sesion_id", nullable = false, updatable = false)
    private UUID sesionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25, updatable = false)
    private TipoMovimientoCaja tipo;

    @Column(nullable = false, updatable = false)
    private short signo;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false, length = 20, updatable = false)
    private MetodoPagoCaja metodoPago;

    @Column(nullable = false, updatable = false)
    private BigDecimal monto;

    @Column(nullable = false, updatable = false)
    private BigDecimal propina;

    @Column(name = "origen_tipo", length = 20, updatable = false)
    private String origenTipo;

    @Column(name = "origen_id", updatable = false)
    private UUID origenId;

    @Column(name = "documento_ref", length = 40, updatable = false)
    private String documentoRef;

    @Column(length = 200, updatable = false)
    private String concepto;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @Column(name = "autorizado_por", updatable = false)
    private UUID autorizadoPor;

    @Column(name = "idempotency_key", nullable = false, length = 120, updatable = false)
    private String idempotencyKey;

    @CreationTimestamp
    @Column(name = "ocurrido_en", nullable = false, updatable = false)
    private OffsetDateTime ocurridoEn;

    protected MovimientoDeCaja() {
    }

    /** Criterio 2: la base declarada al abrir entra como primer movimiento. */
    public static MovimientoDeCaja apertura(UUID negocioId, UUID sesionId, UUID usuarioId,
            BigDecimal monto) {
        MovimientoDeCaja m = base(negocioId, sesionId, usuarioId,
                sesionId + ":APERTURA", "Base de apertura");
        m.tipo = TipoMovimientoCaja.APERTURA;
        m.signo = 1;
        m.metodoPago = MetodoPagoCaja.EFECTIVO;
        m.monto = monto;
        return m;
    }

    /**
     * Criterios 1-3: un cobro de venta, comanda o reserva entra a la caja. El
     * {@code idempotencyKey} determinista más el índice {@code uq_mov_caja_idem}
     * impiden el doble registro (criterio 4).
     */
    public static MovimientoDeCaja deCobro(UUID negocioId, UUID sesionId, UUID usuarioId,
            TipoMovimientoCaja tipo, String origenTipo, UUID origenId, String documentoRef,
            String concepto, MetodoPagoCaja metodoPago, BigDecimal monto, BigDecimal propina,
            String idempotencyKey) {
        MovimientoDeCaja m = base(negocioId, sesionId, usuarioId, idempotencyKey, concepto);
        m.tipo = tipo;
        m.signo = 1;
        m.metodoPago = metodoPago;
        m.monto = monto;
        m.propina = propina == null || propina.signum() < 0 ? BigDecimal.ZERO : propina;
        m.origenTipo = origenTipo;
        m.origenId = origenId;
        m.documentoRef = documentoRef;
        return m;
    }

    private static MovimientoDeCaja base(UUID negocioId, UUID sesionId, UUID usuarioId,
            String idempotencyKey, String concepto) {
        MovimientoDeCaja m = new MovimientoDeCaja();
        m.id = UUID.randomUUID();
        m.negocioId = negocioId;
        m.sesionId = sesionId;
        m.usuarioId = usuarioId;
        m.idempotencyKey = idempotencyKey;
        m.concepto = concepto;
        m.propina = BigDecimal.ZERO;
        return m;
    }

    /** Aporte al efectivo esperado: {@code signo * monto} si es efectivo, 0 si no. */
    public BigDecimal aporteEfectivo() {
        return metodoPago.esEfectivo()
                ? monto.multiply(BigDecimal.valueOf(signo)) : BigDecimal.ZERO;
    }

    public UUID getId() {
        return id;
    }

    public TipoMovimientoCaja getTipo() {
        return tipo;
    }

    public MetodoPagoCaja getMetodoPago() {
        return metodoPago;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public short getSigno() {
        return signo;
    }

    public BigDecimal getPropina() {
        return propina;
    }

    public String getOrigenTipo() {
        return origenTipo;
    }

    public UUID getOrigenId() {
        return origenId;
    }

    public String getDocumentoRef() {
        return documentoRef;
    }

    public String getConcepto() {
        return concepto;
    }

    public java.time.OffsetDateTime getOcurridoEn() {
        return ocurridoEn;
    }

    public UUID getSesionId() {
        return sesionId;
    }
}
