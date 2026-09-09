package com.regenta.menu.domain;

import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import com.regenta.comun.errores.ReglaDeNegocioException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Un grupo de opciones para un plato (HU-078): "Término de la carne", "Extras".
 * {@code minSelecciones}/{@code maxSelecciones} acotan cuántas se pueden elegir;
 * un grupo con {@code min > 0} es obligatorio. La base rechaza
 * {@code max < min} con el CHECK {@code ck_selecciones} (criterio 4).
 */
@Entity
@Table(name = "grupos_modificadores")
public class GrupoDeModificadores {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(name = "min_selecciones", nullable = false)
    private short minSelecciones;

    @Column(name = "max_selecciones", nullable = false)
    private short maxSelecciones;

    /** Columna generada por la base: {@code min_selecciones > 0}. */
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(nullable = false, insertable = false, updatable = false)
    private boolean obligatorio;

    @Column(nullable = false)
    private boolean activo;

    protected GrupoDeModificadores() {
    }

    public static GrupoDeModificadores crear(UUID negocioId, String nombre, int minSelecciones,
            int maxSelecciones) {
        GrupoDeModificadores g = new GrupoDeModificadores();
        g.id = UUID.randomUUID();
        g.negocioId = negocioId;
        g.activo = true;
        g.aplicar(nombre, minSelecciones, maxSelecciones);
        return g;
    }

    public void editar(String nombre, int minSelecciones, int maxSelecciones) {
        aplicar(nombre, minSelecciones, maxSelecciones);
    }

    private void aplicar(String nombre, int minSelecciones, int maxSelecciones) {
        if (minSelecciones < 0) {
            throw new ReglaDeNegocioException("El mínimo de selecciones no puede ser negativo");
        }
        if (maxSelecciones < 1) {
            throw new ReglaDeNegocioException("El máximo de selecciones debe ser al menos 1");
        }
        if (maxSelecciones < minSelecciones) {
            throw new ReglaDeNegocioException(
                    "El máximo de selecciones no puede ser menor que el mínimo");
        }
        this.nombre = nombre;
        this.minSelecciones = (short) minSelecciones;
        this.maxSelecciones = (short) maxSelecciones;
    }

    public void activar() {
        this.activo = true;
    }

    public void desactivar() {
        this.activo = false;
    }

    public boolean esObligatorio() {
        return minSelecciones > 0;
    }

    /** Si elegir {@code cuantos} opciones respeta el mínimo y el máximo del grupo. */
    public boolean permiteSeleccion(int cuantos) {
        return cuantos >= minSelecciones && cuantos <= maxSelecciones;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public String getNombre() {
        return nombre;
    }

    public int getMinSelecciones() {
        return minSelecciones;
    }

    public int getMaxSelecciones() {
        return maxSelecciones;
    }

    public boolean isObligatorio() {
        return obligatorio;
    }

    public boolean isActivo() {
        return activo;
    }
}
