package com.regenta.inventario.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Un lote de un producto que maneja lotes. HU-031.
 *
 * <p>El codigo de lote es unico por producto dentro del negocio
 * ({@code uq_lote}). El saldo de cada lote por bodega vive en
 * {@code existencias_lote}, no aqui.
 */
@Entity
@Table(name = "lotes")
public class Lote {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "producto_id", nullable = false, updatable = false)
    private UUID productoId;

    @Column(name = "codigo_lote", nullable = false, length = 60, updatable = false)
    private String codigoLote;

    @Column(name = "fecha_fabricacion")
    private LocalDate fechaFabricacion;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(name = "registro_sanitario", length = 60)
    private String registroSanitario;

    @Column(name = "costo_unitario")
    private BigDecimal costoUnitario;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    protected Lote() {
    }

    public static Lote nuevo(UUID negocioId, UUID productoId, String codigoLote) {
        Lote lote = new Lote();
        lote.id = UUID.randomUUID();
        lote.negocioId = negocioId;
        lote.productoId = productoId;
        lote.codigoLote = codigoLote;
        return lote;
    }

    /** Fija los datos sanitarios del lote. Se completan al recibir la mercancia. */
    public void datosDeRecepcion(LocalDate fechaFabricacion, LocalDate fechaVencimiento,
            String registroSanitario, BigDecimal costoUnitario) {
        if (fechaFabricacion != null) {
            this.fechaFabricacion = fechaFabricacion;
        }
        if (fechaVencimiento != null) {
            this.fechaVencimiento = fechaVencimiento;
        }
        if (registroSanitario != null && !registroSanitario.isBlank()) {
            this.registroSanitario = registroSanitario.trim();
        }
        if (costoUnitario != null) {
            this.costoUnitario = costoUnitario;
        }
    }

    /** Vencido = ya paso su fecha de vencimiento. El dia del vencimiento aun vende. */
    public boolean estaVencido(LocalDate hoy) {
        return fechaVencimiento != null && hoy.isAfter(fechaVencimiento);
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getProductoId() {
        return productoId;
    }

    public String getCodigoLote() {
        return codigoLote;
    }

    public LocalDate getFechaFabricacion() {
        return fechaFabricacion;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public String getRegistroSanitario() {
        return registroSanitario;
    }

    public BigDecimal getCostoUnitario() {
        return costoUnitario;
    }
}
