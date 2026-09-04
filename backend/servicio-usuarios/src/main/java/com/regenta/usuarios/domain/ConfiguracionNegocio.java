package com.regenta.usuarios.domain;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Los datos con los que salen las facturas y con los que calculan los precios.
 * Uno por negocio: la clave primaria es el negocio.
 */
@Entity
@Table(name = "configuracion_negocio")
public class ConfiguracionNegocio {

    @Id
    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(length = 200)
    private String direccion;

    @Column(length = 80)
    private String ciudad;

    @Column(length = 80)
    private String departamento;

    @Column(length = 30)
    private String telefono;

    @Column(length = 150)
    private String email;

    @Column(name = "sitio_web", length = 150)
    private String sitioWeb;

    @Column(name = "regimen_fiscal", length = 40)
    private String regimenFiscal;

    /** Codigos DIAN: O-13, O-15... */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "responsabilidades_fiscales", nullable = false)
    private List<String> responsabilidadesFiscales = new ArrayList<>();

    @Column(name = "codigo_postal", length = 15)
    private String codigoPostal;

    @Column(name = "decimales_moneda", nullable = false)
    private short decimalesMoneda;

    @Column(name = "formato_fecha", nullable = false, length = 20)
    private String formatoFecha;

    @Column(name = "precios_incluyen_impuesto", nullable = false)
    private boolean preciosIncluyenImpuesto;

    @Column(name = "impuesto_default_id")
    private UUID impuestoDefaultId;

    @Column(name = "politica_stock_negativo", nullable = false)
    private boolean politicaStockNegativo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> preferencias = new LinkedHashMap<>();

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    @Version
    @Column(nullable = false)
    private long version;

    protected ConfiguracionNegocio() {
    }

    /** La configuracion nace con el negocio, con los valores de su pais. */
    public static ConfiguracionNegocio inicial(UUID negocioId) {
        ConfiguracionNegocio configuracion = new ConfiguracionNegocio();
        configuracion.negocioId = negocioId;
        configuracion.decimalesMoneda = 2;
        configuracion.formatoFecha = "dd/MM/yyyy";
        configuracion.preciosIncluyenImpuesto = true;
        configuracion.politicaStockNegativo = false;
        return configuracion;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getCiudad() {
        return ciudad;
    }

    public String getDepartamento() {
        return departamento;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getEmail() {
        return email;
    }

    public String getSitioWeb() {
        return sitioWeb;
    }

    public String getRegimenFiscal() {
        return regimenFiscal;
    }

    public List<String> getResponsabilidadesFiscales() {
        return List.copyOf(responsabilidadesFiscales);
    }

    public String getCodigoPostal() {
        return codigoPostal;
    }

    public short getDecimalesMoneda() {
        return decimalesMoneda;
    }

    public String getFormatoFecha() {
        return formatoFecha;
    }

    public boolean isPreciosIncluyenImpuesto() {
        return preciosIncluyenImpuesto;
    }

    public UUID getImpuestoDefaultId() {
        return impuestoDefaultId;
    }

    public boolean isPoliticaStockNegativo() {
        return politicaStockNegativo;
    }

    public Map<String, Object> getPreferencias() {
        return Map.copyOf(preferencias);
    }

    public long getVersion() {
        return version;
    }

    public void actualizarContacto(String direccion, String ciudad, String departamento,
            String telefono, String email, String sitioWeb, String codigoPostal, String logoUrl) {
        this.direccion = direccion;
        this.ciudad = ciudad;
        this.departamento = departamento;
        this.telefono = telefono;
        this.email = email;
        this.sitioWeb = sitioWeb;
        this.codigoPostal = codigoPostal;
        this.logoUrl = logoUrl;
    }

    public void actualizarFiscal(String regimenFiscal, List<String> responsabilidades) {
        this.regimenFiscal = regimenFiscal;
        this.responsabilidadesFiscales = new ArrayList<>(
                responsabilidades == null ? List.of() : responsabilidades);
    }

    public void actualizarOperacion(short decimalesMoneda, String formatoFecha,
            boolean preciosIncluyenImpuesto, boolean politicaStockNegativo) {
        this.decimalesMoneda = decimalesMoneda;
        this.formatoFecha = formatoFecha;
        this.preciosIncluyenImpuesto = preciosIncluyenImpuesto;
        this.politicaStockNegativo = politicaStockNegativo;
    }

    public void fijarImpuestoPorDefecto(UUID impuestoId) {
        this.impuestoDefaultId = impuestoId;
    }

    public void guardarPreferencias(Map<String, Object> preferencias) {
        this.preferencias = new LinkedHashMap<>(preferencias == null ? Map.of() : preferencias);
    }
}
