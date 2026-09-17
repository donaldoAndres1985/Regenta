package com.regenta.comandas.domain;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Qué proporción de una línea de la comanda va en una cuenta (HU-089). Una
 * bebida compartida entre dos cuentas queda con {@code proporcion} 0.5 en cada
 * una: la suma de sus filas siempre da 1 (100%), sin que nadie escriba el
 * número — lo reparte el servicio entre las cuentas que marcan esa línea.
 */
@Entity
@Table(name = "cuenta_lineas")
@IdClass(CuentaLineaId.class)
public class CuentaLinea {

    @Id
    @Column(name = "cuenta_id", nullable = false, updatable = false)
    private UUID cuentaId;

    @Id
    @Column(name = "comanda_linea_id", nullable = false, updatable = false)
    private UUID comandaLineaId;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(nullable = false)
    private BigDecimal proporcion;

    @Column(nullable = false)
    private BigDecimal monto;

    protected CuentaLinea() {
    }

    public static CuentaLinea de(UUID negocioId, UUID cuentaId, UUID comandaLineaId) {
        CuentaLinea cl = new CuentaLinea();
        cl.negocioId = negocioId;
        cl.cuentaId = cuentaId;
        cl.comandaLineaId = comandaLineaId;
        cl.proporcion = BigDecimal.ONE;
        cl.monto = BigDecimal.ZERO;
        return cl;
    }

    public void fijarReparto(BigDecimal proporcion, BigDecimal monto) {
        this.proporcion = proporcion;
        this.monto = monto;
    }

    public UUID getCuentaId() {
        return cuentaId;
    }

    public UUID getComandaLineaId() {
        return comandaLineaId;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public BigDecimal getProporcion() {
        return proporcion;
    }

    public BigDecimal getMonto() {
        return monto;
    }
}
