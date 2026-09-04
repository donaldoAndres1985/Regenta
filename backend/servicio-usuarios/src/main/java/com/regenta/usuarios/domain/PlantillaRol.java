package com.regenta.usuarios.domain;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

/**
 * Plantilla de rol del catalogo. El motor de permisos no cambia entre
 * patrones: lo unico que cambia son las plantillas que cada negocio instancia.
 */
@Entity
@Table(name = "plantillas_rol")
public class PlantillaRol {

    @Id
    private UUID id;

    @Column(nullable = false, length = 40)
    private String codigo;

    @Column(nullable = false, length = 60)
    private String nombre;

    /** {@code null} = comun a los tres patrones. */
    @Column(length = 30)
    private String patron;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "plantilla_rol_permisos", joinColumns = @JoinColumn(name = "plantilla_id"))
    @Column(name = "permiso_codigo", nullable = false, length = 80)
    private Set<String> permisos = new LinkedHashSet<>();

    protected PlantillaRol() {
    }

    public UUID getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getPatron() {
        return patron;
    }

    public Set<String> getPermisos() {
        return Set.copyOf(permisos);
    }

    public boolean esComun() {
        return patron == null;
    }
}
