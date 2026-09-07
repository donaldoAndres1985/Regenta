package com.regenta.inventario.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Que campo extra exige (o permite) una categoria en sus productos. Es el
 * contrato que valida el JSONB {@code productos.atributos}: la tesis del
 * sistema es que ferreteria, papeleria y drogueria cambian estas filas, no el
 * codigo (HU-027).
 */
@Entity
@Table(name = "atributos_categoria")
public class AtributoCategoria {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "categoria_id", nullable = false, updatable = false)
    private UUID categoriaId;

    @Column(name = "nombre_campo", nullable = false, length = 50)
    private String nombreCampo;

    @Column(nullable = false, length = 80)
    private String etiqueta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoAtributo tipo;

    @Column(nullable = false)
    private boolean obligatorio;

    @Column(length = 20)
    private String unidad;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> opciones;

    @Column(name = "valor_default", columnDefinition = "text")
    private String valorDefault;

    @Column(name = "validacion_regex", columnDefinition = "text")
    private String validacionRegex;

    @Column(name = "valor_min")
    private BigDecimal valorMin;

    @Column(name = "valor_max")
    private BigDecimal valorMax;

    @Column(nullable = false)
    private boolean heredable;

    @Column(nullable = false)
    private int orden;

    @Column(nullable = false)
    private boolean activo;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    protected AtributoCategoria() {
    }

    public static AtributoCategoria nuevo(UUID negocioId, UUID categoriaId, String nombreCampo,
            String etiqueta, TipoAtributo tipo) {
        AtributoCategoria atributo = new AtributoCategoria();
        atributo.id = UUID.randomUUID();
        atributo.negocioId = negocioId;
        atributo.categoriaId = categoriaId;
        atributo.nombreCampo = nombreCampo;
        atributo.etiqueta = etiqueta;
        atributo.tipo = tipo;
        atributo.obligatorio = false;
        atributo.heredable = true;
        atributo.orden = 0;
        atributo.activo = true;
        return atributo;
    }

    /** Copia este atributo hacia una subcategoria (HU-026, criterio 4). */
    public AtributoCategoria heredarEn(UUID subcategoriaId) {
        AtributoCategoria copia = nuevo(negocioId, subcategoriaId, nombreCampo, etiqueta, tipo);
        copia.obligatorio = obligatorio;
        copia.unidad = unidad;
        copia.opciones = opciones == null ? null : List.copyOf(opciones);
        copia.valorDefault = valorDefault;
        copia.validacionRegex = validacionRegex;
        copia.valorMin = valorMin;
        copia.valorMax = valorMax;
        copia.heredable = heredable;
        copia.orden = orden;
        return copia;
    }

    public void configurar(boolean obligatorio, String unidad, List<String> opciones,
            String valorDefault, String validacionRegex, BigDecimal valorMin, BigDecimal valorMax,
            Boolean heredable, Integer orden) {
        this.obligatorio = obligatorio;
        this.unidad = unidad;
        this.opciones = opciones;
        this.valorDefault = valorDefault;
        this.validacionRegex = validacionRegex;
        this.valorMin = valorMin;
        this.valorMax = valorMax;
        if (heredable != null) {
            this.heredable = heredable;
        }
        if (orden != null) {
            this.orden = orden;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getCategoriaId() {
        return categoriaId;
    }

    public String getNombreCampo() {
        return nombreCampo;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public TipoAtributo getTipo() {
        return tipo;
    }

    public boolean isObligatorio() {
        return obligatorio;
    }

    public String getUnidad() {
        return unidad;
    }

    public List<String> getOpciones() {
        return opciones;
    }

    public String getValorDefault() {
        return valorDefault;
    }

    public String getValidacionRegex() {
        return validacionRegex;
    }

    public BigDecimal getValorMin() {
        return valorMin;
    }

    public BigDecimal getValorMax() {
        return valorMax;
    }

    public boolean isHeredable() {
        return heredable;
    }

    public int getOrden() {
        return orden;
    }

    public boolean isActivo() {
        return activo;
    }
}
