package com.regenta.compras.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

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
 * Lo que el negocio le debe a un proveedor por una recepción (HU-048 criterio
 * 6). El vencimiento sale del plazo de crédito del proveedor: {@code
 * fecha_emision + dias_credito}. Los pagos y el listado por antigüedad son
 * HU-049.
 */
@Entity
@Table(name = "cuentas_por_pagar")
public class CuentaPorPagar {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "proveedor_id", nullable = false, updatable = false)
    private UUID proveedorId;

    @Column(name = "recepcion_id", updatable = false)
    private UUID recepcionId;

    @Column(name = "numero_factura", nullable = false, length = 40, updatable = false)
    private String numeroFactura;

    @Column(nullable = false)
    private BigDecimal monto;

    @Column(nullable = false)
    private BigDecimal saldo;

    @Column(name = "fecha_emision", nullable = false, updatable = false)
    private LocalDate fechaEmision;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCuentaPorPagar estado;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Version
    @Column(nullable = false)
    private long version;

    protected CuentaPorPagar() {
    }

    /** Criterio 6: el vencimiento es la fecha de emisión más el plazo del proveedor. */
    public static CuentaPorPagar abrirPorRecepcion(UUID negocioId, UUID proveedorId,
            UUID recepcionId, String numeroFactura, BigDecimal monto, LocalDate fechaEmision,
            int diasCredito) {
        CuentaPorPagar c = new CuentaPorPagar();
        c.id = UUID.randomUUID();
        c.negocioId = negocioId;
        c.proveedorId = proveedorId;
        c.recepcionId = recepcionId;
        c.numeroFactura = numeroFactura;
        c.monto = monto;
        c.saldo = monto;
        c.fechaEmision = fechaEmision;
        c.fechaVencimiento = fechaEmision.plusDays(Math.max(0, diasCredito));
        c.estado = EstadoCuentaPorPagar.PENDIENTE;
        return c;
    }

    /**
     * Criterio 2 de HU-049: baja el saldo y, si queda algo, deja la cuenta en
     * {@code PARCIAL}; si se salda, en {@code PAGADA}. Pagar de más o pagar una
     * cuenta ya saldada/anulada se rechaza. El CHECK {@code ck_saldo_cxp} lo
     * vuelve a cortar en la base.
     */
    public void registrarPago(BigDecimal monto) {
        if (monto == null || monto.signum() <= 0) {
            throw new ReglaDeNegocioException("El pago debe ser mayor que cero");
        }
        if (estado == EstadoCuentaPorPagar.PAGADA || estado == EstadoCuentaPorPagar.ANULADA) {
            throw new ConflictoDeEstadoException("La cuenta ya está " + estado);
        }
        if (monto.compareTo(saldo) > 0) {
            throw new ReglaDeNegocioException(
                    "El pago " + monto + " supera el saldo pendiente " + saldo);
        }
        this.saldo = saldo.subtract(monto);
        this.estado = saldo.signum() == 0
                ? EstadoCuentaPorPagar.PAGADA : EstadoCuentaPorPagar.PARCIAL;
    }

    /** Criterio 3: vencida = tiene saldo y su vencimiento ya pasó. */
    public boolean estaVencida(LocalDate hoy) {
        return saldo.signum() > 0 && estado != EstadoCuentaPorPagar.ANULADA
                && fechaVencimiento.isBefore(hoy);
    }

    public long diasDeMora(LocalDate hoy) {
        return estaVencida(hoy) ? ChronoUnit.DAYS.between(fechaVencimiento, hoy) : 0;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getProveedorId() {
        return proveedorId;
    }

    public UUID getRecepcionId() {
        return recepcionId;
    }

    public String getNumeroFactura() {
        return numeroFactura;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public EstadoCuentaPorPagar getEstado() {
        return estado;
    }
}
