package com.regenta.inventario.domain;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Una línea de un ajuste: un producto, lo que dice el sistema y lo que se
 * contó. La {@code diferencia} la calcula la base (columna generada), no la
 * aplicación (criterio 1 de HU-033).
 */
@Entity
@Table(name = "ajuste_lineas")
public class AjusteLinea {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "ajuste_id", nullable = false, updatable = false)
    private UUID ajusteId;

    @Column(name = "producto_id", nullable = false, updatable = false)
    private UUID productoId;

    @Column(name = "lote_id", updatable = false)
    private UUID loteId;

    @Column(name = "cantidad_sistema", nullable = false, updatable = false)
    private BigDecimal cantidadSistema;

    @Column(name = "cantidad_fisica", nullable = false, updatable = false)
    private BigDecimal cantidadFisica;

    @Column(name = "diferencia", insertable = false, updatable = false)
    private BigDecimal diferencia;

    @Column(name = "costo_unitario")
    private BigDecimal costoUnitario;

    protected AjusteLinea() {
    }

    public static AjusteLinea de(UUID negocioId, UUID ajusteId, UUID productoId, UUID loteId,
            BigDecimal cantidadSistema, BigDecimal cantidadFisica, BigDecimal costoUnitario) {
        AjusteLinea l = new AjusteLinea();
        l.id = UUID.randomUUID();
        l.negocioId = negocioId;
        l.ajusteId = ajusteId;
        l.productoId = productoId;
        l.loteId = loteId;
        l.cantidadSistema = cantidadSistema;
        l.cantidadFisica = cantidadFisica;
        l.costoUnitario = costoUnitario;
        return l;
    }

    /** Lo contado menos lo que decía el sistema. Positivo = sobra, negativo = falta. */
    public BigDecimal diferenciaCalculada() {
        return cantidadFisica.subtract(cantidadSistema);
    }

    public UUID getId() {
        return id;
    }

    public UUID getAjusteId() {
        return ajusteId;
    }

    public UUID getProductoId() {
        return productoId;
    }

    public UUID getLoteId() {
        return loteId;
    }

    public BigDecimal getCantidadSistema() {
        return cantidadSistema;
    }

    public BigDecimal getCantidadFisica() {
        return cantidadFisica;
    }

    public BigDecimal getDiferencia() {
        return diferencia;
    }
}
