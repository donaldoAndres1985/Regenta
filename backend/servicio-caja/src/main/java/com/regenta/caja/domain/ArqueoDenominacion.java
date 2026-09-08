package com.regenta.caja.domain;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import com.regenta.comun.errores.ReglaDeNegocioException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Una fila del conteo de efectivo por denominación al cerrar la caja (HU-062).
 * {@code subtotal} = {@code denominacion * cantidad} lo calcula la base (columna
 * generada). Una denominación no se repite en la misma sesión
 * ({@code uq_denominacion}).
 */
@Entity
@Table(name = "arqueo_denominaciones")
public class ArqueoDenominacion {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "sesion_id", nullable = false, updatable = false)
    private UUID sesionId;

    @Column(nullable = false, updatable = false)
    private BigDecimal denominacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private TipoDenominacion tipo;

    @Column(nullable = false, updatable = false)
    private int cantidad;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(insertable = false, updatable = false)
    private BigDecimal subtotal;

    protected ArqueoDenominacion() {
    }

    public static ArqueoDenominacion de(UUID negocioId, UUID sesionId, BigDecimal denominacion,
            TipoDenominacion tipo, int cantidad) {
        if (denominacion == null || denominacion.signum() <= 0) {
            throw new ReglaDeNegocioException("La denominación debe ser mayor que cero");
        }
        if (cantidad < 0) {
            throw new ReglaDeNegocioException("La cantidad no puede ser negativa");
        }
        ArqueoDenominacion a = new ArqueoDenominacion();
        a.id = UUID.randomUUID();
        a.negocioId = negocioId;
        a.sesionId = sesionId;
        a.denominacion = denominacion;
        a.tipo = tipo;
        a.cantidad = cantidad;
        return a;
    }

    public BigDecimal getDenominacion() {
        return denominacion;
    }

    public TipoDenominacion getTipo() {
        return tipo;
    }

    public int getCantidad() {
        return cantidad;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }
}
