package com.regenta.caja.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * HU-140 · una línea del arqueo por método de pago, guardada una sola vez al cerrar la sesión
 * (I12). {@code diferencia} la calcula la base (I4b); {@code cuadrable} queda congelado (I8b).
 */
@Entity
@Table(name = "arqueo_metodos", schema = "caja")
public class ArqueoMetodo {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "sesion_id", nullable = false, updatable = false)
    private UUID sesionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false, length = 20, updatable = false)
    private MetodoPagoCaja metodoPago;

    @Column(nullable = false, updatable = false)
    private boolean cuadrable;

    @Column(nullable = false, updatable = false)
    private BigDecimal esperado;

    @Column(nullable = false, updatable = false)
    private BigDecimal contado;

    @Generated(event = EventType.INSERT)
    @Column(insertable = false, updatable = false)
    private BigDecimal diferencia;

    @Column(nullable = false, updatable = false)
    private int movimientos;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    protected ArqueoMetodo() {
    }

    public static ArqueoMetodo de(UUID negocioId, UUID sesionId, LineaDeArqueo linea) {
        ArqueoMetodo a = new ArqueoMetodo();
        a.id = UUID.randomUUID();
        a.negocioId = negocioId;
        a.sesionId = sesionId;
        a.metodoPago = linea.metodo();
        a.cuadrable = linea.cuadrable();
        a.esperado = linea.esperado();
        a.contado = linea.contado();
        a.diferencia = linea.diferencia();
        a.movimientos = (int) linea.movimientos();
        return a;
    }

    public LineaDeArqueo comoLinea() {
        return new LineaDeArqueo(metodoPago, cuadrable, esperado, contado, movimientos);
    }

    public MetodoPagoCaja getMetodoPago() {
        return metodoPago;
    }

    public BigDecimal getDiferencia() {
        return diferencia;
    }
}
