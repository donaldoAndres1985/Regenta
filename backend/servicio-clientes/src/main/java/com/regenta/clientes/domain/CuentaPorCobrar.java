package com.regenta.clientes.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
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
 * Una cuenta por cobrar: lo que un cliente debe por una venta (o reserva,
 * comanda o factura) a plazo. El {@code saldo} arranca igual al {@code monto} y
 * baja con cada recaudo; el CHECK {@code ck_saldo} de la base garantiza que
 * nunca quede negativo ni mayor que el monto.
 */
@Entity
@Table(name = "cuentas_por_cobrar")
public class CuentaPorCobrar {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "cliente_id", nullable = false, updatable = false)
    private UUID clienteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen_tipo", nullable = false, length = 20)
    private OrigenDeCuenta origenTipo;

    @Column(name = "origen_id", nullable = false)
    private UUID origenId;

    @Column(name = "documento_ref", length = 40)
    private String documentoRef;

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
    private EstadoCuenta estado;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Version
    @Column(nullable = false)
    private long version;

    protected CuentaPorCobrar() {
    }

    public static CuentaPorCobrar crear(UUID negocioId, UUID clienteId, OrigenDeCuenta origenTipo,
            UUID origenId, String documentoRef, BigDecimal monto, LocalDate fechaEmision,
            LocalDate fechaVencimiento) {
        if (monto == null || monto.signum() <= 0) {
            throw new ReglaDeNegocioException("El monto de la cuenta debe ser mayor que cero");
        }
        CuentaPorCobrar cuenta = new CuentaPorCobrar();
        cuenta.id = UUID.randomUUID();
        cuenta.negocioId = negocioId;
        cuenta.clienteId = clienteId;
        cuenta.origenTipo = origenTipo;
        cuenta.origenId = origenId;
        cuenta.documentoRef = documentoRef;
        cuenta.monto = monto;
        cuenta.saldo = monto;
        cuenta.fechaEmision = fechaEmision == null ? LocalDate.now() : fechaEmision;
        cuenta.fechaVencimiento = fechaVencimiento == null
                ? cuenta.fechaEmision : fechaVencimiento;
        cuenta.estado = EstadoCuenta.PENDIENTE;
        return cuenta;
    }

    /**
     * Aplica un recaudo. El monto no puede pasar del saldo. Si lo iguala, la
     * cuenta queda {@link EstadoCuenta#PAGADA} (criterio 4); si no,
     * {@link EstadoCuenta#PARCIAL} (criterio 3).
     */
    public void registrarRecaudo(BigDecimal montoRecaudo) {
        if (montoRecaudo == null || montoRecaudo.signum() <= 0) {
            throw new ReglaDeNegocioException("El recaudo debe ser mayor que cero");
        }
        if (montoRecaudo.compareTo(saldo) > 0) {
            throw new ReglaDeNegocioException("El recaudo de " + montoRecaudo
                    + " excede el saldo pendiente de " + saldo);
        }
        saldo = saldo.subtract(montoRecaudo);
        estado = saldo.signum() == 0 ? EstadoCuenta.PAGADA : EstadoCuenta.PARCIAL;
    }

    /** Días de mora a una fecha: 0 si está pagada o aún no vence (criterio 2). */
    public long diasDeMora(LocalDate hoy) {
        if (estado == EstadoCuenta.PAGADA || !hoy.isAfter(fechaVencimiento)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(fechaVencimiento, hoy);
    }

    public boolean estaVencida(LocalDate hoy) {
        return diasDeMora(hoy) > 0;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getClienteId() {
        return clienteId;
    }

    public OrigenDeCuenta getOrigenTipo() {
        return origenTipo;
    }

    public UUID getOrigenId() {
        return origenId;
    }

    public String getDocumentoRef() {
        return documentoRef;
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

    public EstadoCuenta getEstado() {
        return estado;
    }
}
