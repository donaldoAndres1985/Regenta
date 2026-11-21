package com.regenta.compras.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.regenta.shared.web.ConflictoDeEstadoException;
import com.regenta.shared.web.ReglaDeNegocioException;

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
@Table(name = "cuentas_por_pagar", schema = "compras")
public class CuentaPorPagar {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "proveedor_id", nullable = false, updatable = false)
    private UUID proveedorId;

    @Column(name = "recepcion_id", updatable = false)
    private UUID recepcionId;

    /** Nulo mientras la cuenta está en espera de factura: llegó con remisión (HU-170). */
    @Column(name = "numero_factura", length = 40)
    private String numeroFactura;

    @Column(nullable = false)
    private BigDecimal monto;

    @Column(nullable = false)
    private BigDecimal saldo;

    @Column(name = "fecha_emision", nullable = false)
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
     * HU-170 · La mercancía llegó con remisión: la cuenta nace sin número de factura, «en espera de
     * factura», con un vencimiento provisional desde la fecha de recepción. Ya se le puede abonar.
     */
    public static CuentaPorPagar abrirEnEsperaDeFactura(UUID negocioId, UUID proveedorId,
            UUID recepcionId, BigDecimal monto, LocalDate fechaRecepcion, int diasCredito) {
        return abrirPorRecepcion(negocioId, proveedorId, recepcionId, null, monto, fechaRecepcion,
                diasCredito);
    }

    public boolean enEsperaDeFactura() {
        return numeroFactura == null;
    }

    /**
     * HU-170 C3 · Llegó la factura: se registra su número y su fecha, se quita la marca y el
     * vencimiento se recalcula desde la fecha de la factura. Solo una cuenta en espera (409).
     */
    public void registrarFactura(String numero, LocalDate fecha, int diasCredito) {
        if (!enEsperaDeFactura()) {
            throw new ConflictoDeEstadoException(
                    "Esta cuenta ya tiene la factura " + numeroFactura + " registrada");
        }
        if (estado == EstadoCuentaPorPagar.ANULADA) {
            throw new ConflictoDeEstadoException("La cuenta está anulada");
        }
        if (numero == null || numero.isBlank()) {
            throw new ReglaDeNegocioException("Indica el número de la factura del proveedor");
        }
        if (fecha == null) {
            throw new ReglaDeNegocioException("Indica la fecha de la factura del proveedor");
        }
        this.numeroFactura = numero.trim();
        this.fechaEmision = fecha;
        this.fechaVencimiento = fecha.plusDays(Math.max(0, diasCredito));
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
    /**
     * HU-172 · Se anuló un pago: lo que abonó vuelve al saldo y el estado se recalcula — si vuelve a
     * deberse todo, {@code PENDIENTE}; si no, {@code PARCIAL}.
     */
    public void revertirPago(BigDecimal monto) {
        if (estado == EstadoCuentaPorPagar.ANULADA) {
            throw new ConflictoDeEstadoException("La cuenta está anulada: no se le revierte un pago");
        }
        BigDecimal nuevo = saldo.add(monto);
        if (nuevo.compareTo(this.monto) > 0) {
            throw new ReglaDeNegocioException("Revertir ese pago dejaría el saldo por encima de la cuenta");
        }
        this.saldo = nuevo;
        this.estado = saldo.compareTo(this.monto) == 0
                ? EstadoCuentaPorPagar.PENDIENTE : EstadoCuentaPorPagar.PARCIAL;
    }

    /**
     * HU-177 C2 · La recepción de esta factura se anuló: la cuenta también. Con un abono no se puede
     * (409): primero se anulan los pagos.
     */
    public void anularPorRecepcion() {
        if (estado == EstadoCuentaPorPagar.ANULADA) {
            return;
        }
        if (saldo.compareTo(monto) < 0) {
            throw new ConflictoDeEstadoException("Primero anula los pagos de esta factura.");
        }
        this.estado = EstadoCuentaPorPagar.ANULADA;
    }

    /**
     * HU-178 C2 · Una nota crédito por una devolución baja el saldo hasta donde alcance; devuelve lo que
     * rebajó (lo demás queda a favor). Saldada queda {@code PAGADA}. Una cuenta anulada no se rebaja.
     */
    public BigDecimal aplicarNotaCredito(BigDecimal valor) {
        if (estado == EstadoCuentaPorPagar.ANULADA || valor == null || valor.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal aplicado = valor.min(saldo);
        this.saldo = saldo.subtract(aplicado);
        if (saldo.signum() == 0) {
            this.estado = EstadoCuentaPorPagar.PAGADA;
        } else if (aplicado.signum() > 0) {
            this.estado = EstadoCuentaPorPagar.PARCIAL;
        }
        return aplicado;
    }

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
