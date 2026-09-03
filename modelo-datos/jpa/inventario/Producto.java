package com.regenta.inventario.dominio;

import com.regenta.comun.dominio.EntidadTenant;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Agregado raíz del catálogo del patrón Venta directa.
 *
 * `atributos` es lo que permite que ferretería, papelería y droguería usen
 * esta misma clase sin código nuevo: los campos que varían por categoría
 * viven en el JSONB y se validan contra `atributos_categoria`.
 *
 * OJO: aquí NO hay campo `stock`. El stock es (producto, bodega) y vive en
 * la entidad Existencia. Ver el hallazgo #1 del documento del modelo.
 */
@Entity
@Table(name = "productos", schema = "inventario")
public class Producto extends EntidadTenant {

    @Column(name = "sku", nullable = false, length = 60)  private String sku;
    @Column(name = "codigo_barras", length = 60)          private String codigoBarras;
    @Column(name = "nombre", nullable = false, length = 180) private String nombre;
    @Column(name = "descripcion") private String descripcion;

    /** FK real: la categoría vive en ESTE servicio. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(name = "unidad_medida_id", nullable = false) private UUID unidadMedidaId;
    @Column(name = "marca_id") private UUID marcaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoProducto tipo = TipoProducto.BIEN;

    @Column(name = "precio_venta",   nullable = false, precision = 14, scale = 4) private BigDecimal precioVenta = BigDecimal.ZERO;
    @Column(name = "costo_promedio", nullable = false, precision = 14, scale = 4) private BigDecimal costoPromedio = BigDecimal.ZERO;

    /**
     * Referencia LÓGICA a core_identidad.impuestos: otro microservicio, otra
     * base de datos. Sin @ManyToOne y sin FK física, por diseño.
     */
    @Column(name = "impuesto_id") private UUID impuestoId;

    @Column(name = "controla_stock", nullable = false) private boolean controlaStock = true;
    @Column(name = "stock_minimo", nullable = false, precision = 18, scale = 6) private BigDecimal stockMinimo = BigDecimal.ZERO;
    @Column(name = "maneja_lotes",  nullable = false) private boolean manejaLotes = false;
    @Column(name = "maneja_series", nullable = false) private boolean manejaSeries = false;
    @Column(name = "perecedero",    nullable = false) private boolean perecedero = false;

    /** Hibernate 6 mapea JSONB nativo: no hace falta la librería hibernate-types. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "atributos", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> atributos = new HashMap<>();

    @Column(name = "activo", nullable = false) private boolean activo = true;

    /** Idempotencia de la sincronización offline. */
    @Column(name = "origen_offline_id", updatable = false) private UUID origenOfflineId;

    public enum TipoProducto { BIEN, SERVICIO, COMBO, INSUMO }

    /** Invariante: si es perecedero, obligatoriamente maneja lotes. */
    public void marcarPerecedero(boolean valor) {
        this.perecedero = valor;
        if (valor) this.manejaLotes = true;
    }

    public Map<String, Object> getAtributos() { return atributos; }
    public String getSku() { return sku; }
    public String getNombre() { return nombre; }
    public Categoria getCategoria() { return categoria; }
    public BigDecimal getPrecioVenta() { return precioVenta; }
    public boolean isManejaLotes() { return manejaLotes; }
}
