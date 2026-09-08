package com.regenta.recursos.domain;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Una habitación, cancha o consultorio concreto (HU-065). Su `codigo` es único
 * por negocio ({@code uq_recurso_codigo}); sus campos variables van en el JSONB
 * {@code atributos}, validados contra su tipo (HU-064).
 */
@Entity
@Table(name = "recursos")
public class Recurso {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "sucursal_id")
    private UUID sucursalId;

    @Column(name = "tipo_recurso_id", nullable = false, updatable = false)
    private UUID tipoRecursoId;

    @Column(nullable = false, length = 30, updatable = false)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(columnDefinition = "text")
    private String descripcion;

    @Column(nullable = false)
    private short capacidad;

    @Column(length = 20)
    private String piso;

    @Column(length = 60)
    private String zona;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRecurso estado;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> atributos;

    @Column(name = "imagen_url", columnDefinition = "text")
    private String imagenUrl;

    @Column(nullable = false)
    private boolean activo;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    @Column(name = "eliminado_en")
    private OffsetDateTime eliminadoEn;

    @Version
    @Column(nullable = false)
    private long version;

    protected Recurso() {
    }

    public static Recurso crear(UUID negocioId, UUID sucursalId, UUID tipoRecursoId, String codigo,
            String nombre, String descripcion, int capacidad, String piso, String zona,
            Map<String, Object> atributos, String imagenUrl) {
        Recurso r = new Recurso();
        r.id = UUID.randomUUID();
        r.negocioId = negocioId;
        r.tipoRecursoId = tipoRecursoId;
        r.codigo = codigo;
        r.estado = EstadoRecurso.DISPONIBLE;
        r.activo = true;
        r.aplicar(sucursalId, nombre, descripcion, capacidad, piso, zona, atributos, imagenUrl);
        return r;
    }

    public void editar(UUID sucursalId, String nombre, String descripcion, int capacidad,
            String piso, String zona, Map<String, Object> atributos, String imagenUrl) {
        aplicar(sucursalId, nombre, descripcion, capacidad, piso, zona, atributos, imagenUrl);
    }

    private void aplicar(UUID sucursalId, String nombre, String descripcion, int capacidad,
            String piso, String zona, Map<String, Object> atributos, String imagenUrl) {
        this.sucursalId = sucursalId;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.capacidad = (short) Math.max(1, capacidad);
        this.piso = piso;
        this.zona = zona;
        this.atributos = atributos == null ? Map.of() : atributos;
        this.imagenUrl = imagenUrl;
    }

    /** Criterio 2: cambiar a MANTENIMIENTO (u otro no-DISPONIBLE) lo saca de los disponibles. */
    public void cambiarEstado(EstadoRecurso nuevo) {
        this.estado = nuevo;
    }

    public void eliminar() {
        this.activo = false;
        this.eliminadoEn = OffsetDateTime.now();
    }

    public boolean estaEliminado() {
        return eliminadoEn != null;
    }

    public boolean disponibleParaReservar() {
        return activo && !estaEliminado() && estado.disponibleParaReservar();
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

    public UUID getTipoRecursoId() {
        return tipoRecursoId;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public String getPiso() {
        return piso;
    }

    public String getZona() {
        return zona;
    }

    public EstadoRecurso getEstado() {
        return estado;
    }

    public Map<String, Object> getAtributos() {
        return atributos;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public boolean isActivo() {
        return activo;
    }
}
