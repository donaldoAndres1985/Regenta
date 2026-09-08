package com.regenta.recursos.domain;

import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Qué campo extra exige (o permite) un tipo de recurso en sus recursos (HU-064).
 * Es el contrato que valida el JSONB {@code recursos.atributos} — el espejo de
 * {@code atributos_categoria} en Inventario.
 */
@Entity
@Table(name = "atributos_tipo_recurso")
public class AtributoTipoRecurso {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "tipo_recurso_id", nullable = false, updatable = false)
    private UUID tipoRecursoId;

    @Column(name = "nombre_campo", nullable = false, length = 50)
    private String nombreCampo;

    @Column(nullable = false, length = 80)
    private String etiqueta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoAtributoRecurso tipo;

    @Column(nullable = false)
    private boolean obligatorio;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> opciones;

    @Column(nullable = false)
    private int orden;

    @Column(nullable = false)
    private boolean activo;

    protected AtributoTipoRecurso() {
    }

    public static AtributoTipoRecurso nuevo(UUID negocioId, UUID tipoRecursoId, String nombreCampo,
            String etiqueta, TipoAtributoRecurso tipo, boolean obligatorio, List<String> opciones,
            int orden) {
        AtributoTipoRecurso a = new AtributoTipoRecurso();
        a.id = UUID.randomUUID();
        a.negocioId = negocioId;
        a.tipoRecursoId = tipoRecursoId;
        a.nombreCampo = nombreCampo;
        a.activo = true;
        a.configurar(etiqueta, tipo, obligatorio, opciones, orden);
        return a;
    }

    public void configurar(String etiqueta, TipoAtributoRecurso tipo, boolean obligatorio,
            List<String> opciones, int orden) {
        this.etiqueta = etiqueta;
        this.tipo = tipo;
        this.obligatorio = obligatorio;
        this.opciones = opciones == null ? null : List.copyOf(opciones);
        this.orden = orden;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTipoRecursoId() {
        return tipoRecursoId;
    }

    public String getNombreCampo() {
        return nombreCampo;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public TipoAtributoRecurso getTipo() {
        return tipo;
    }

    public boolean isObligatorio() {
        return obligatorio;
    }

    public List<String> getOpciones() {
        return opciones;
    }

    public int getOrden() {
        return orden;
    }

    public boolean isActivo() {
        return activo;
    }
}
