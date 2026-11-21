package com.regenta.compras.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * HU-178 · La nota crédito de una devolución (I27). Primero baja el saldo de la cuenta de la
 * recepción ({@code aplicadoACuenta}); lo que no cabe ({@code excedente}) queda como saldo a favor del
 * negocio con el proveedor, que se va usando en los pagos (HU-171 C5) y vuelve si se anula el pago.
 */
@Entity
@Table(name = "notas_credito_proveedor", schema = "compras")
public class NotaCreditoProveedor {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "proveedor_id", nullable = false, updatable = false)
    private UUID proveedorId;

    @Column(name = "devolucion_id", nullable = false, updatable = false)
    private UUID devolucionId;

    @Column(name = "cuenta_id", updatable = false)
    private UUID cuentaId;

    @Column(nullable = false, updatable = false)
    private BigDecimal monto;

    @Column(name = "aplicado_a_cuenta", nullable = false, updatable = false)
    private BigDecimal aplicadoACuenta;

    @Column(nullable = false, updatable = false)
    private BigDecimal excedente;

    @Column(name = "saldo_a_favor", nullable = false)
    private BigDecimal saldoAFavor;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Version
    @Column(nullable = false)
    private long version;

    protected NotaCreditoProveedor() {
    }

    /**
     * La nota por {@code monto}: rebaja de la cuenta lo que su saldo permita y deja el resto a favor.
     * Sin cuenta (anulada o inexistente), todo queda a favor.
     */
    public static NotaCreditoProveedor emitir(UUID negocioId, DevolucionAProveedor devolucion, CuentaPorPagar cuenta,
            BigDecimal monto) {
        NotaCreditoProveedor n = new NotaCreditoProveedor();
        n.id = UUID.randomUUID();
        n.negocioId = negocioId;
        n.proveedorId = devolucion.getProveedorId();
        n.devolucionId = devolucion.getId();
        n.cuentaId = cuenta == null ? null : cuenta.getId();
        n.monto = monto;
        n.aplicadoACuenta = cuenta == null ? BigDecimal.ZERO : cuenta.aplicarNotaCredito(monto);
        n.excedente = monto.subtract(n.aplicadoACuenta);
        n.saldoAFavor = n.excedente;
        return n;
    }

    /** Usa hasta {@code maximo} del saldo a favor; devuelve lo usado. */
    public BigDecimal usar(BigDecimal maximo) {
        BigDecimal usado = saldoAFavor.min(maximo).max(BigDecimal.ZERO);
        saldoAFavor = saldoAFavor.subtract(usado);
        return usado;
    }

    /** HU-172 · Se anuló un pago que la usó: recupera hasta {@code monto}; devuelve lo recuperado. */
    public BigDecimal recuperar(BigDecimal monto) {
        BigDecimal cabe = excedente.subtract(saldoAFavor).min(monto).max(BigDecimal.ZERO);
        saldoAFavor = saldoAFavor.add(cabe);
        return cabe;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProveedorId() {
        return proveedorId;
    }

    public UUID getDevolucionId() {
        return devolucionId;
    }

    public UUID getCuentaId() {
        return cuentaId;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public BigDecimal getAplicadoACuenta() {
        return aplicadoACuenta;
    }

    public BigDecimal getExcedente() {
        return excedente;
    }

    public BigDecimal getSaldoAFavor() {
        return saldoAFavor;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }
}
