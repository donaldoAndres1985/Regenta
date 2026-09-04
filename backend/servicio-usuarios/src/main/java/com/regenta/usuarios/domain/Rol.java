package com.regenta.usuarios.domain;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Un rol del negocio, con los permisos que le dio su administrador.
 *
 * <p>El Core solo conoce "rol" y "permiso". Ningun nombre de rol de un patron
 * concreto se escribe en el codigo: los roles salen de plantillas y a partir de
 * ahi son del negocio.
 */
@Entity
@Table(name = "roles")
public class Rol {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    /** De que plantilla salio, si salio de una. */
    @Column(name = "plantilla_id")
    private UUID plantillaId;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column
    private String descripcion;

    /** Un rol de sistema no se borra: el negocio se quedaria sin administrador. */
    @Column(name = "es_sistema", nullable = false)
    private boolean esSistema;

    @Column(nullable = false)
    private boolean activo;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rol_permisos", joinColumns = @JoinColumn(name = "rol_id"))
    @Column(name = "permiso_codigo", nullable = false, length = 80)
    private Set<String> permisos = new LinkedHashSet<>();

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    @Version
    @Column(nullable = false)
    private long version;

    protected Rol() {
    }

    public static Rol desdePlantilla(UUID negocioId, PlantillaRol plantilla, boolean esSistema) {
        Rol rol = new Rol();
        rol.id = UUID.randomUUID();
        rol.negocioId = negocioId;
        rol.plantillaId = plantilla.getId();
        rol.nombre = plantilla.getNombre();
        rol.esSistema = esSistema;
        rol.activo = true;
        rol.permisos = new LinkedHashSet<>(plantilla.getPermisos());
        return rol;
    }

    public static Rol propio(UUID negocioId, String nombre, String descripcion,
            Collection<String> permisos) {
        Rol rol = new Rol();
        rol.id = UUID.randomUUID();
        rol.negocioId = negocioId;
        rol.nombre = nombre;
        rol.descripcion = descripcion;
        rol.esSistema = false;
        rol.activo = true;
        rol.permisos = new LinkedHashSet<>(permisos);
        return rol;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getPlantillaId() {
        return plantillaId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isEsSistema() {
        return esSistema;
    }

    public boolean isActivo() {
        return activo;
    }

    public Set<String> getPermisos() {
        return Set.copyOf(permisos);
    }

    public boolean puede(String permiso) {
        return permisos.contains(permiso);
    }

    public long getVersion() {
        return version;
    }

    public void renombrar(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public void reemplazarPermisos(Collection<String> nuevos) {
        this.permisos.clear();
        this.permisos.addAll(nuevos);
    }

    public void desactivar() {
        this.activo = false;
    }
}
