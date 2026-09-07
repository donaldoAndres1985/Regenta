package com.regenta.inventario.domain;

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
 * Un producto del catalogo. Los campos que varian por tipo de negocio no son
 * columnas: van en el JSONB {@code atributos}, validados contra
 * {@code atributos_categoria} en el servicio antes de guardar (HU-028). El
 * CHECK {@code ck_lotes_perecedero} de la base exige que un perecedero maneje
 * lotes.
 */
@Entity
@Table(name = "productos")
public class Producto {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(nullable = false, length = 60)
    private String sku;

    @Column(name = "codigo_barras", length = 60)
    private String codigoBarras;

    @Column(nullable = false, length = 180)
    private String nombre;

    @Column(columnDefinition = "text")
    private String descripcion;

    @Column(name = "categoria_id", nullable = false)
    private UUID categoriaId;

    @Column(name = "marca_id")
    private UUID marcaId;

    @Column(name = "unidad_medida_id", nullable = false)
    private UUID unidadMedidaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoProducto tipo;

    @Column(name = "precio_venta", nullable = false)
    private BigDecimal precioVenta;

    @Column(name = "controla_stock", nullable = false)
    private boolean controlaStock;

    @Column(name = "stock_minimo", nullable = false)
    private BigDecimal stockMinimo;

    @Column(name = "stock_maximo")
    private BigDecimal stockMaximo;

    @Column(name = "maneja_lotes", nullable = false)
    private boolean manejaLotes;

    @Column(name = "maneja_series", nullable = false)
    private boolean manejaSeries;

    @Column(nullable = false)
    private boolean perecedero;

    @Column(name = "permite_venta_sin_stock", nullable = false)
    private boolean permiteVentaSinStock;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> atributos = new LinkedHashMap<>();

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "origen_offline_id")
    private UUID origenOfflineId;

    @Column(name = "creado_por")
    private UUID creadoPor;

    @Column(name = "actualizado_por")
    private UUID actualizadoPor;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    @Version
    @Column(nullable = false)
    private long version;

    protected Producto() {
    }

    public static Producto nuevo(UUID negocioId, String sku, String nombre, UUID categoriaId,
            UUID unidadMedidaId, UUID creadoPor) {
        Producto producto = new Producto();
        producto.id = UUID.randomUUID();
        producto.negocioId = negocioId;
        producto.sku = sku;
        producto.nombre = nombre;
        producto.categoriaId = categoriaId;
        producto.unidadMedidaId = unidadMedidaId;
        producto.tipo = TipoProducto.BIEN;
        producto.precioVenta = BigDecimal.ZERO;
        producto.controlaStock = true;
        producto.stockMinimo = BigDecimal.ZERO;
        producto.manejaLotes = false;
        producto.manejaSeries = false;
        producto.perecedero = false;
        producto.permiteVentaSinStock = false;
        producto.activo = true;
        producto.creadoPor = creadoPor;
        return producto;
    }

    public void datosBasicos(String descripcion, UUID marcaId, TipoProducto tipo,
            BigDecimal precioVenta, String codigoBarras) {
        this.descripcion = descripcion;
        this.marcaId = marcaId;
        if (tipo != null) {
            this.tipo = tipo;
        }
        if (precioVenta != null) {
            this.precioVenta = precioVenta;
        }
        this.codigoBarras = (codigoBarras == null || codigoBarras.isBlank()) ? null : codigoBarras;
    }

    public void configuracionDeStock(Boolean controlaStock, BigDecimal stockMinimo,
            BigDecimal stockMaximo, boolean manejaLotes, boolean manejaSeries, boolean perecedero,
            boolean permiteVentaSinStock) {
        if (controlaStock != null) {
            this.controlaStock = controlaStock;
        }
        if (stockMinimo != null) {
            this.stockMinimo = stockMinimo;
        }
        this.stockMaximo = stockMaximo;
        this.manejaLotes = manejaLotes;
        this.manejaSeries = manejaSeries;
        this.perecedero = perecedero;
        this.permiteVentaSinStock = permiteVentaSinStock;
    }

    public void fijarAtributos(Map<String, Object> atributos) {
        this.atributos = atributos == null ? new LinkedHashMap<>() : new LinkedHashMap<>(atributos);
    }

    public void tocar(UUID usuarioId) {
        this.actualizadoPor = usuarioId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public String getSku() {
        return sku;
    }

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public UUID getCategoriaId() {
        return categoriaId;
    }

    public UUID getMarcaId() {
        return marcaId;
    }

    public UUID getUnidadMedidaId() {
        return unidadMedidaId;
    }

    public TipoProducto getTipo() {
        return tipo;
    }

    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }

    public boolean isControlaStock() {
        return controlaStock;
    }

    public BigDecimal getStockMinimo() {
        return stockMinimo;
    }

    public BigDecimal getStockMaximo() {
        return stockMaximo;
    }

    public boolean isManejaLotes() {
        return manejaLotes;
    }

    public boolean isManejaSeries() {
        return manejaSeries;
    }

    public boolean isPerecedero() {
        return perecedero;
    }

    public boolean isPermiteVentaSinStock() {
        return permiteVentaSinStock;
    }

    public Map<String, Object> getAtributos() {
        return atributos;
    }

    public boolean isActivo() {
        return activo;
    }

    public long getVersion() {
        return version;
    }
}
