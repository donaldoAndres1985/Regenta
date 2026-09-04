package com.regenta.usuarios.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Un punto fisico del negocio.
 *
 * <p>Multi-sucursal se vende en Empresarial, pero {@code sucursal_id} existe en
 * inventario, ventas y caja desde el dia uno: agregarla despues obliga a
 * reescribir todos los indices y todas las consultas.
 */
@Entity
@Table(name = "sucursales")
public class Sucursal {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(nullable = false, length = 20)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 200)
    private String direccion;

    @Column(length = 80)
    private String ciudad;

    @Column(length = 30)
    private String telefono;

    @Column(name = "es_principal", nullable = false)
    private boolean esPrincipal;

    @Column(nullable = false)
    private boolean activa;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    @Version
    @Column(nullable = false)
    private long version;

    protected Sucursal() {
    }

    public static Sucursal nueva(UUID negocioId, String codigo, String nombre, boolean principal) {
        Sucursal sucursal = new Sucursal();
        sucursal.id = UUID.randomUUID();
        sucursal.negocioId = negocioId;
        sucursal.codigo = codigo;
        sucursal.nombre = nombre;
        sucursal.esPrincipal = principal;
        sucursal.activa = true;
        return sucursal;
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

    public String getDireccion() {
        return direccion;
    }

    public String getCiudad() {
        return ciudad;
    }

    public String getTelefono() {
        return telefono;
    }

    public boolean isEsPrincipal() {
        return esPrincipal;
    }

    public boolean isActiva() {
        return activa;
    }

    public long getVersion() {
        return version;
    }

    public void actualizar(String nombre, String direccion, String ciudad, String telefono) {
        this.nombre = nombre;
        this.direccion = direccion;
        this.ciudad = ciudad;
        this.telefono = telefono;
    }

    /** Solo puede haber una principal: lo garantiza un indice unico parcial. */
    public void hacerPrincipal() {
        this.esPrincipal = true;
    }

    public void dejarDeSerPrincipal() {
        this.esPrincipal = false;
    }

    public void desactivar() {
        this.activa = false;
    }

    public void activar() {
        this.activa = true;
    }
}
