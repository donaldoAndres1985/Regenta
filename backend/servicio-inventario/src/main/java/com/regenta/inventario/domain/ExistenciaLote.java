package com.regenta.inventario.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * El saldo de un lote en una bodega. HU-031.
 *
 * <p>Es el desglose por lote de {@code existencias}: la suma de los lotes de un
 * producto en una bodega debe cuadrar con {@code existencias.cantidad}. El CHECK
 * {@code cantidad >= 0} de la base es la ultima palabra contra un sobregiro.
 */
@Entity
@Table(name = "existencias_lote")
@IdClass(ExistenciaLoteId.class)
public class ExistenciaLote {

    @Id
    @Column(name = "lote_id")
    private UUID loteId;

    @Id
    @Column(name = "bodega_id")
    private UUID bodegaId;

    @Column(name = "negocio_id", nullable = false)
    private UUID negocioId;

    @Column(nullable = false)
    private BigDecimal cantidad;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    protected ExistenciaLote() {
    }

    public static ExistenciaLote enCero(UUID negocioId, UUID loteId, UUID bodegaId) {
        ExistenciaLote existencia = new ExistenciaLote();
        existencia.negocioId = negocioId;
        existencia.loteId = loteId;
        existencia.bodegaId = bodegaId;
        existencia.cantidad = BigDecimal.ZERO;
        return existencia;
    }

    public BigDecimal saldoSiAplico(int signo, BigDecimal cantidad) {
        return this.cantidad.add(cantidad.multiply(BigDecimal.valueOf(signo)));
    }

    public void aplicar(int signo, BigDecimal cantidad) {
        this.cantidad = saldoSiAplico(signo, cantidad);
    }

    public UUID getLoteId() {
        return loteId;
    }

    public UUID getBodegaId() {
        return bodegaId;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }
}
