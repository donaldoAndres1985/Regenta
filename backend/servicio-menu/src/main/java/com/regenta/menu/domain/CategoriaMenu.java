package com.regenta.menu.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Una sección de la carta (HU-076): "Entradas", "Fuertes", "Bebidas". Se muestran
 * en el {@code orden} que fija el negocio (criterio 2).
 */
@Entity
@Table(name = "categorias_menu")
public class CategoriaMenu {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "carta_id", nullable = false, updatable = false)
    private UUID cartaId;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(columnDefinition = "text")
    private String descripcion;

    @Column(nullable = false)
    private int orden;

    @Column(length = 40)
    private String icono;

    @Column(nullable = false)
    private boolean activa;

    protected CategoriaMenu() {
    }

    public static CategoriaMenu crear(UUID negocioId, UUID cartaId, String nombre,
            String descripcion, int orden, String icono) {
        CategoriaMenu c = new CategoriaMenu();
        c.id = UUID.randomUUID();
        c.negocioId = negocioId;
        c.cartaId = cartaId;
        c.activa = true;
        c.aplicar(nombre, descripcion, orden, icono);
        return c;
    }

    public void editar(String nombre, String descripcion, int orden, String icono) {
        aplicar(nombre, descripcion, orden, icono);
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

    private void aplicar(String nombre, String descripcion, int orden, String icono) {
        this.nombre = nombre;
        this.descripcion = descripcion == null || descripcion.isBlank() ? null : descripcion.trim();
        this.orden = Math.max(0, orden);
        this.icono = icono == null || icono.isBlank() ? null : icono.trim();
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getCartaId() {
        return cartaId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getOrden() {
        return orden;
    }

    public String getIcono() {
        return icono;
    }

    public boolean isActiva() {
        return activa;
    }
}
