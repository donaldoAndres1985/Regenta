package com.regenta.mesas.domain;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Una zona del salón (HU-081): "Salón", "Terraza", "Barra", "VIP". Agrupa mesas
 * y ordena cómo se dibuja el plano.
 */
@Entity
@Table(name = "zonas")
public class Zona {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "sucursal_id")
    private UUID sucursalId;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(nullable = false)
    private int orden;

    /** Color hex {@code #RRGGBB} para pintar la zona en el plano. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 7, columnDefinition = "char(7)")
    private String color;

    @Column(nullable = false)
    private boolean activa;

    protected Zona() {
    }

    public static Zona crear(UUID negocioId, UUID sucursalId, String nombre, Integer orden,
            String color) {
        Zona z = new Zona();
        z.id = UUID.randomUUID();
        z.negocioId = negocioId;
        z.sucursalId = sucursalId;
        z.activa = true;
        z.aplicar(nombre, orden, color);
        return z;
    }

    public void editar(String nombre, Integer orden, String color) {
        aplicar(nombre, orden, color);
    }

    private void aplicar(String nombre, Integer orden, String color) {
        this.nombre = nombre == null ? null : nombre.trim();
        this.orden = orden == null ? this.orden : Math.max(0, orden);
        this.color = color == null || color.isBlank() ? null : color.trim();
    }

    public void reordenar(int orden) {
        this.orden = Math.max(0, orden);
    }

    public void activar() {
        this.activa = true;
    }

    public void desactivar() {
        this.activa = false;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getSucursalId() {
        return sucursalId;
    }

    public String getNombre() {
        return nombre;
    }

    public int getOrden() {
        return orden;
    }

    public String getColor() {
        return color == null ? null : color.trim();
    }

    public boolean isActiva() {
        return activa;
    }
}
