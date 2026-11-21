package com.regenta.compras.domain;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** HU-171 · Lo que un pago a proveedor abonó a una de sus cuentas (I26). */
@Entity
@Table(name = "pago_aplicaciones", schema = "compras")
public class AplicacionDePago {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "pago_id", nullable = false, updatable = false)
    private UUID pagoId;

    @Column(name = "cuenta_id", nullable = false, updatable = false)
    private UUID cuentaId;

    @Column(nullable = false, updatable = false)
    private BigDecimal monto;

    protected AplicacionDePago() {
    }

    public static AplicacionDePago de(UUID negocioId, UUID pagoId, UUID cuentaId, BigDecimal monto) {
        AplicacionDePago a = new AplicacionDePago();
        a.id = UUID.randomUUID();
        a.negocioId = negocioId;
        a.pagoId = pagoId;
        a.cuentaId = cuentaId;
        a.monto = monto;
        return a;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPagoId() {
        return pagoId;
    }

    public UUID getCuentaId() {
        return cuentaId;
    }

    public BigDecimal getMonto() {
        return monto;
    }
}
