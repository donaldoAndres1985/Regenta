package com.regenta.compras.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

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
