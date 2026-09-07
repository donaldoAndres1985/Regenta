package com.regenta.inventario.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Una categoria de producto, con las palabras del negocio y no las del sistema
 * (HU-026). Puede colgar de otra: la jerarquia se guarda como {@code ruta}
 * (materialized path) y {@code nivel}.
 *
 * <p>Nunca viene precargada en el codigo: cada negocio crea las suyas.
 */
@Entity
@Table(name = "categorias")
public class Categoria {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "categoria_padre_id")
    private UUID categoriaPadreId;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(columnDefinition = "text")
    private String descripcion;

    @Column(length = 500)
    private String ruta;

    @Column(nullable = false)
    private short nivel;

    @Column(length = 40)
    private String icono;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 7)
    private String color;

    @Column(nullable = false)
    private int orden;

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

    protected Categoria() {
    }

    /** Categoria raiz de un negocio. */
    public static Categoria raiz(UUID negocioId, String nombre) {
        return crear(negocioId, null, nombre, (short) 0, nombre);
    }

    /** Subcategoria: hereda nivel y ruta del padre. */
    public static Categoria hija(UUID negocioId, Categoria padre, String nombre) {
        return crear(negocioId, padre.id, nombre, (short) (padre.nivel + 1),
                (padre.ruta == null ? padre.nombre : padre.ruta) + "/" + nombre);
    }

    private static Categoria crear(UUID negocioId, UUID padreId, String nombre, short nivel,
            String ruta) {
        Categoria categoria = new Categoria();
        categoria.id = UUID.randomUUID();
        categoria.negocioId = negocioId;
        categoria.categoriaPadreId = padreId;
        categoria.nombre = nombre;
        categoria.nivel = nivel;
        categoria.ruta = ruta;
        categoria.orden = 0;
        categoria.activa = true;
        return categoria;
    }

    public void describir(String descripcion, String icono, String color, Integer orden) {
        this.descripcion = descripcion;
        this.icono = icono;
        this.color = color;
        if (orden != null) {
            this.orden = orden;
        }
    }

    public void renombrar(String nombre) {
        this.nombre = nombre;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getCategoriaPadreId() {
        return categoriaPadreId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getRuta() {
        return ruta;
    }

    public short getNivel() {
        return nivel;
    }

    public String getIcono() {
        return icono;
    }

    public String getColor() {
        return color;
    }

    public int getOrden() {
        return orden;
    }

    public boolean isActiva() {
        return activa;
    }

    public boolean esRaiz() {
        return categoriaPadreId == null;
    }

    public long getVersion() {
        return version;
    }
}
