package com.regenta.usuarios.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Un impuesto del negocio. Codigos DIAN: 01 IVA, 04 INC.
 *
 * <p>El porcentaje no se cambia una vez usado en documentos emitidos: una
 * factura de marzo tiene que seguir cuadrando en diciembre. Se crea otro.
 */
@Entity
@Table(name = "impuestos")
public class Impuesto {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(nullable = false, length = 20)
    private String codigo;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false)
    private BigDecimal porcentaje;

    @Column(name = "aplica_sobre", nullable = false, length = 20)
    private String aplicaSobre;

    @Column(nullable = false)
    private boolean activo;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    protected Impuesto() {
    }

    public static Impuesto nuevo(UUID negocioId, String codigo, String nombre, String tipo,
            BigDecimal porcentaje, String aplicaSobre) {
        Impuesto impuesto = new Impuesto();
        impuesto.id = UUID.randomUUID();
        impuesto.negocioId = negocioId;
        impuesto.codigo = codigo;
        impuesto.nombre = nombre;
        impuesto.tipo = tipo;
        impuesto.porcentaje = porcentaje;
        impuesto.aplicaSobre = aplicaSobre == null ? "BASE" : aplicaSobre;
        impuesto.activo = true;
        return impuesto;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public BigDecimal getPorcentaje() {
        return porcentaje;
    }

    public String getAplicaSobre() {
        return aplicaSobre;
    }

    public boolean isActivo() {
        return activo;
    }

    /** Se renombra y se apaga, pero el porcentaje no se toca. */
    public void renombrar(String nombre) {
        this.nombre = nombre;
    }

    public void desactivar() {
        this.activo = false;
    }
}
