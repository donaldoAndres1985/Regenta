package com.regenta.compras.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
 * Un proveedor del negocio: a quién comprarle, con qué plazo y con qué cupo
 * (HU-046). La identidad es {@code (tipo_documento, numero_documento)}, única
 * por negocio ({@code uq_proveedor_doc}).
 */
@Entity
@Table(name = "proveedores")
public class Proveedor {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 10)
    private TipoDocumentoProveedor tipoDocumento;

    @Column(name = "numero_documento", nullable = false, length = 30)
    private String numeroDocumento;

    @Column(name = "razon_social", nullable = false, length = 200)
    private String razonSocial;

    @Column(name = "nombre_comercial", length = 150)
    private String nombreComercial;

    @Column(name = "contacto_nombre", length = 120)
    private String contactoNombre;

    @Column(length = 150)
    private String email;

    @Column(length = 30)
    private String telefono;

    @Column(length = 200)
    private String direccion;

    @Column(length = 80)
    private String ciudad;

    @Column(name = "dias_credito", nullable = false)
    private short diasCredito;

    @Column(name = "cupo_credito", nullable = false)
    private BigDecimal cupoCredito;

    @Column(name = "saldo_pendiente", nullable = false)
    private BigDecimal saldoPendiente;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private String moneda;

    @Column
    private Short calificacion;

    @Column(columnDefinition = "text")
    private String notas;

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

    protected Proveedor() {
    }

    public static Proveedor crear(UUID negocioId, TipoDocumentoProveedor tipoDocumento,
            String numeroDocumento, String razonSocial, String nombreComercial,
            String contactoNombre, String email, String telefono, String direccion, String ciudad,
            int diasCredito, BigDecimal cupoCredito, Short calificacion, String notas) {
        Proveedor p = new Proveedor();
        p.id = UUID.randomUUID();
        p.negocioId = negocioId;
        p.tipoDocumento = tipoDocumento;
        p.numeroDocumento = numeroDocumento;
        p.razonSocial = razonSocial;
        p.nombreComercial = nombreComercial;
        p.contactoNombre = contactoNombre;
        p.email = email;
        p.telefono = telefono;
        p.direccion = direccion;
        p.ciudad = ciudad;
        p.diasCredito = (short) Math.max(0, diasCredito);
        p.cupoCredito = cupoCredito == null || cupoCredito.signum() < 0
                ? BigDecimal.ZERO : cupoCredito;
        p.saldoPendiente = BigDecimal.ZERO;
        p.moneda = "COP";
        p.calificacion = calificacion;
        p.notas = notas;
        p.activo = true;
        return p;
    }

    public void editar(String razonSocial, String nombreComercial, String contactoNombre,
            String email, String telefono, String direccion, String ciudad, int diasCredito,
            BigDecimal cupoCredito, Short calificacion, String notas) {
        this.razonSocial = razonSocial;
        this.nombreComercial = nombreComercial;
        this.contactoNombre = contactoNombre;
        this.email = email;
        this.telefono = telefono;
        this.direccion = direccion;
        this.ciudad = ciudad;
        this.diasCredito = (short) Math.max(0, diasCredito);
        this.cupoCredito = cupoCredito == null || cupoCredito.signum() < 0
                ? BigDecimal.ZERO : cupoCredito;
        this.calificacion = calificacion;
        this.notas = notas;
    }

    public void desactivar() {
        this.activo = false;
        this.eliminadoEn = OffsetDateTime.now();
    }

    public boolean estaEliminado() {
        return eliminadoEn != null;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public TipoDocumentoProveedor getTipoDocumento() {
        return tipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public String getNombreComercial() {
        return nombreComercial;
    }

    public String getContactoNombre() {
        return contactoNombre;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getCiudad() {
        return ciudad;
    }

    public short getDiasCredito() {
        return diasCredito;
    }

    public BigDecimal getCupoCredito() {
        return cupoCredito;
    }

    public BigDecimal getSaldoPendiente() {
        return saldoPendiente;
    }

    public Short getCalificacion() {
        return calificacion;
    }

    public String getNotas() {
        return notas;
    }

    public boolean isActivo() {
        return activo;
    }
}
