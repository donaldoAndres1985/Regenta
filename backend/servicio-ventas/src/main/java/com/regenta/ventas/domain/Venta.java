package com.regenta.ventas.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Una venta del patrón Venta directa. HU-037.
 *
 * <p>Se arma en {@code BORRADOR} agregando líneas; los totales se calculan y se
 * guardan en columnas, nunca se derivan al consultar. Una vez fuera de
 * {@code BORRADOR} las líneas no se tocan (criterio 5). El número es
 * consecutivo por negocio y lo asigna el {@code AsignadorDeConsecutivos}.
 */
@Entity
@Table(name = "ventas")
public class Venta {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "bodega_id", nullable = false, updatable = false)
    private UUID bodegaId;

    @Column(nullable = false, length = 30, updatable = false)
    private String numero;

    @Column(name = "cliente_id")
    private UUID clienteId;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @Column(name = "lista_precios_id")
    private UUID listaPreciosId;

    @Column(nullable = false, length = 20)
    private String canal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private EstadoVenta estado;

    @Column(nullable = false)
    private OffsetDateTime fecha;

    @Column(nullable = false)
    private BigDecimal subtotal;

    @Column(name = "descuento_total", nullable = false)
    private BigDecimal descuentoTotal;

    @Column(name = "base_gravable", nullable = false)
    private BigDecimal baseGravable;

    @Column(name = "impuesto_total", nullable = false)
    private BigDecimal impuestoTotal;

    @Column(nullable = false)
    private BigDecimal total;

    @Column(name = "costo_total", nullable = false)
    private BigDecimal costoTotal;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private String moneda;

    @Column(name = "forma_pago", nullable = false, length = 20)
    private String formaPago;

    @Column(name = "saldo_pendiente", nullable = false)
    private BigDecimal saldoPendiente;

    @Column(name = "fecha_vencimiento")
    private java.time.LocalDate fechaVencimiento;

    @Column(name = "estado_factura", length = 20)
    private String estadoFactura;

    @Column(name = "anulada_en")
    private OffsetDateTime anuladaEn;

    @Column(name = "anulada_por")
    private UUID anuladaPor;

    @Column(name = "motivo_anulacion", columnDefinition = "text")
    private String motivoAnulacion;

    @Column(columnDefinition = "text")
    private String nota;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    @Version
    @Column(nullable = false)
    private long version;

    protected Venta() {
    }

    public static Venta crear(UUID negocioId, UUID bodegaId, String numero, UUID clienteId,
            UUID usuarioId, UUID listaPreciosId, String canal) {
        Venta v = new Venta();
        v.id = UUID.randomUUID();
        v.negocioId = negocioId;
        v.bodegaId = bodegaId;
        v.numero = numero;
        v.clienteId = clienteId;
        v.usuarioId = usuarioId;
        v.listaPreciosId = listaPreciosId;
        v.canal = canal == null || canal.isBlank() ? "MOSTRADOR" : canal;
        v.estado = EstadoVenta.BORRADOR;
        v.fecha = OffsetDateTime.now();
        v.moneda = "COP";
        v.formaPago = "CONTADO";
        v.subtotal = BigDecimal.ZERO;
        v.descuentoTotal = BigDecimal.ZERO;
        v.baseGravable = BigDecimal.ZERO;
        v.impuestoTotal = BigDecimal.ZERO;
        v.total = BigDecimal.ZERO;
        v.costoTotal = BigDecimal.ZERO;
        v.saldoPendiente = BigDecimal.ZERO;
        v.estadoFactura = "NO_APLICA";
        return v;
    }

    public void exigirBorrador(String accion) {
        if (estado != EstadoVenta.BORRADOR) {
            throw new ConflictoDeEstadoException(
                    "La venta " + numero + " esta en " + estado + " y no se puede " + accion);
        }
    }

    public void fijarTotales(BigDecimal subtotal, BigDecimal descuento, BigDecimal base,
            BigDecimal impuesto, BigDecimal total, BigDecimal costo) {
        this.subtotal = subtotal;
        this.descuentoTotal = descuento;
        this.baseGravable = base;
        this.impuestoTotal = impuesto;
        this.total = total;
        this.costoTotal = costo;
    }

    /** BORRADOR → PENDIENTE_STOCK: confirmar arranca la saga de reserva (HU-038). */
    public void aPendienteStock() {
        exigirBorrador("confirmar");
        this.estado = EstadoVenta.PENDIENTE_STOCK;
    }

    /** PENDIENTE_STOCK → CONFIRMADA: la reserva de stock salió bien. */
    public void aConfirmada() {
        exigirEsperandoStock("confirmar");
        this.estado = EstadoVenta.CONFIRMADA;
        this.saldoPendiente = this.total;
    }

    /** PENDIENTE_STOCK → BORRADOR: no hubo stock; vuelve a edición con el motivo. */
    public void volverABorrador(String motivo) {
        exigirEsperandoStock("devolver a borrador");
        this.estado = EstadoVenta.BORRADOR;
        this.nota = motivo;
    }

    private void exigirEsperandoStock(String accion) {
        if (estado != EstadoVenta.PENDIENTE_STOCK) {
            throw new ConflictoDeEstadoException("La venta " + numero + " esta en " + estado
                    + " y no se puede " + accion);
        }
    }

    public void exigirConfirmada(String accion) {
        if (estado != EstadoVenta.CONFIRMADA) {
            throw new ConflictoDeEstadoException("La venta " + numero + " esta en " + estado
                    + " y no se puede " + accion);
        }
    }

    /** Deja el saldo pendiente, la forma de pago y —si es a crédito— el vencimiento (HU-039). */
    public void registrarCobro(BigDecimal saldoPendiente, String formaPago,
            java.time.LocalDate fechaVencimiento) {
        this.saldoPendiente = saldoPendiente;
        this.formaPago = formaPago;
        if (fechaVencimiento != null) {
            this.fechaVencimiento = fechaVencimiento;
        }
    }

    /**
     * CONFIRMADA → ANULADA (HU-041). Nunca borra: la venta queda con su histórico
     * intacto y consta quién, cuándo y por qué. Una venta ya facturada
     * electrónicamente no se anula: hay que emitir una nota crédito.
     */
    public void anular(UUID usuarioId, String motivo) {
        exigirConfirmada("anular");
        if ("EMITIDA".equals(estadoFactura)) {
            throw new ReglaDeNegocioException("La venta " + numero
                    + " ya fue facturada electronicamente: emite una nota credito para revertirla");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaDeNegocioException("Indica el motivo de la anulacion");
        }
        this.estado = EstadoVenta.ANULADA;
        this.anuladaEn = OffsetDateTime.now();
        this.anuladaPor = usuarioId;
        this.motivoAnulacion = motivo.trim();
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getBodegaId() {
        return bodegaId;
    }

    public UUID getClienteId() {
        return clienteId;
    }

    public BigDecimal getSaldoPendiente() {
        return saldoPendiente;
    }

    public String getNumero() {
        return numero;
    }

    public EstadoVenta getEstado() {
        return estado;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDescuentoTotal() {
        return descuentoTotal;
    }

    public BigDecimal getBaseGravable() {
        return baseGravable;
    }

    public BigDecimal getImpuestoTotal() {
        return impuestoTotal;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public BigDecimal getCostoTotal() {
        return costoTotal;
    }
}
