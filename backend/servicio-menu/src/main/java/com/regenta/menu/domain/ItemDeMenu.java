package com.regenta.menu.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
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
 * Un plato, bebida o adicional de la carta (HU-077). Lleva su categoría, su
 * precio, la estación que lo prepara y el tiempo de preparación (criterio 1); el
 * {@code codigo} es único por negocio (criterio 2); alérgenos, apto vegano y
 * nivel de picante van en el JSONB {@code atributos} (criterio 3). Marcarlo no
 * disponible lo deja "agotado" en la carta del mesero (criterio 4).
 */
@Entity
@Table(name = "items_menu")
public class ItemDeMenu {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "categoria_menu_id", nullable = false)
    private UUID categoriaMenuId;

    @Column(name = "estacion_id")
    private UUID estacionId;

    @Column(nullable = false, length = 30, updatable = false)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(columnDefinition = "text")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoDeItem tipo;

    @Column(nullable = false)
    private BigDecimal precio;

    @Column(name = "impuesto_id")
    private UUID impuestoId;

    @Column(name = "precio_incluye_impuesto", nullable = false)
    private boolean precioIncluyeImpuesto;

    @Column(name = "costo_estimado", nullable = false)
    private BigDecimal costoEstimado;

    @Column(name = "tiempo_preparacion_min")
    private Short tiempoPreparacionMin;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CursoDeMenu curso;

    @Column(nullable = false)
    private boolean disponible;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> atributos = new LinkedHashMap<>();

    @Column(name = "imagen_url", columnDefinition = "text")
    private String imagenUrl;

    @Column(nullable = false)
    private int orden;

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

    protected ItemDeMenu() {
    }

    public static ItemDeMenu crear(UUID negocioId, UUID categoriaMenuId, UUID estacionId,
            String codigo, String nombre, String descripcion, TipoDeItem tipo, BigDecimal precio,
            UUID impuestoId, boolean precioIncluyeImpuesto, Short tiempoPreparacionMin,
            CursoDeMenu curso, Map<String, Object> atributos, String imagenUrl, int orden) {
        ItemDeMenu i = new ItemDeMenu();
        i.id = UUID.randomUUID();
        i.negocioId = negocioId;
        i.categoriaMenuId = categoriaMenuId;
        i.codigo = codigo.trim();
        i.costoEstimado = BigDecimal.ZERO;
        i.disponible = true;
        i.activo = true;
        i.aplicar(estacionId, nombre, descripcion, tipo, precio, impuestoId, precioIncluyeImpuesto,
                tiempoPreparacionMin, curso, atributos, imagenUrl, orden);
        return i;
    }

    public void editar(UUID estacionId, String nombre, String descripcion, TipoDeItem tipo,
            BigDecimal precio, UUID impuestoId, boolean precioIncluyeImpuesto,
            Short tiempoPreparacionMin, CursoDeMenu curso, Map<String, Object> atributos,
            String imagenUrl, int orden) {
        aplicar(estacionId, nombre, descripcion, tipo, precio, impuestoId, precioIncluyeImpuesto,
                tiempoPreparacionMin, curso, atributos, imagenUrl, orden);
    }

    private void aplicar(UUID estacionId, String nombre, String descripcion, TipoDeItem tipo,
            BigDecimal precio, UUID impuestoId, boolean precioIncluyeImpuesto,
            Short tiempoPreparacionMin, CursoDeMenu curso, Map<String, Object> atributos,
            String imagenUrl, int orden) {
        this.estacionId = estacionId;
        this.nombre = nombre;
        this.descripcion = descripcion == null || descripcion.isBlank() ? null : descripcion.trim();
        this.tipo = tipo == null ? TipoDeItem.PLATO : tipo;
        this.precio = precio == null || precio.signum() < 0 ? BigDecimal.ZERO : precio;
        this.impuestoId = impuestoId;
        this.precioIncluyeImpuesto = precioIncluyeImpuesto;
        this.tiempoPreparacionMin = tiempoPreparacionMin != null && tiempoPreparacionMin < 0
                ? null : tiempoPreparacionMin;
        this.curso = curso;
        this.atributos = atributos == null ? new LinkedHashMap<>() : new LinkedHashMap<>(atributos);
        this.imagenUrl = imagenUrl == null || imagenUrl.isBlank() ? null : imagenUrl.trim();
        this.orden = Math.max(0, orden);
    }

    /** "Se acabó" del día (HU-077 criterio 4). */
    public void marcarDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    public void eliminar(OffsetDateTime cuando) {
        this.activo = false;
        this.eliminadoEn = cuando;
    }

    public boolean estaEliminado() {
        return eliminadoEn != null;
    }

    /** El mesero lo puede agregar a una comanda: activo, no eliminado y disponible. */
    public boolean pedible() {
        return activo && !estaEliminado() && disponible;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getCategoriaMenuId() {
        return categoriaMenuId;
    }

    public UUID getEstacionId() {
        return estacionId;
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

    public TipoDeItem getTipo() {
        return tipo;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public UUID getImpuestoId() {
        return impuestoId;
    }

    public boolean isPrecioIncluyeImpuesto() {
        return precioIncluyeImpuesto;
    }

    public BigDecimal getCostoEstimado() {
        return costoEstimado;
    }

    public Short getTiempoPreparacionMin() {
        return tiempoPreparacionMin;
    }

    public CursoDeMenu getCurso() {
        return curso;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public Map<String, Object> getAtributos() {
        return atributos;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public int getOrden() {
        return orden;
    }

    public boolean isActivo() {
        return activo;
    }

    public long getVersion() {
        return version;
    }
}
