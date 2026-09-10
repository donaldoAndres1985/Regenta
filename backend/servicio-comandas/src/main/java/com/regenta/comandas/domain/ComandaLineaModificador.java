package com.regenta.comandas.domain;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Un modificador elegido para una línea, con copia de su nombre y precio (HU-085). */
@Entity
@Table(name = "comanda_linea_modificadores")
public class ComandaLineaModificador {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "linea_id", nullable = false, updatable = false)
    private UUID lineaId;

    @Column(name = "modificador_id", nullable = false, updatable = false)
    private UUID modificadorId;

    @Column(name = "nombre_snapshot", nullable = false, length = 80, updatable = false)
    private String nombreSnapshot;

    @Column(name = "precio_extra", nullable = false)
    private BigDecimal precioExtra;

    @Column(nullable = false)
    private short cantidad;

    protected ComandaLineaModificador() {
    }

    public static ComandaLineaModificador de(UUID negocioId, UUID lineaId, UUID modificadorId,
            String nombre, BigDecimal precioExtra) {
        ComandaLineaModificador m = new ComandaLineaModificador();
        m.id = UUID.randomUUID();
        m.negocioId = negocioId;
        m.lineaId = lineaId;
        m.modificadorId = modificadorId;
        m.nombreSnapshot = nombre;
        m.precioExtra = precioExtra == null ? BigDecimal.ZERO : precioExtra;
        m.cantidad = 1;
        return m;
    }

    public UUID getId() {
        return id;
    }

    public UUID getLineaId() {
        return lineaId;
    }

    public UUID getModificadorId() {
        return modificadorId;
    }

    public String getNombreSnapshot() {
        return nombreSnapshot;
    }

    public BigDecimal getPrecioExtra() {
        return precioExtra;
    }
}
