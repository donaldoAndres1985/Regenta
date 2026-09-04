package com.regenta.usuarios.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * La raiz del tenant: la fila que se crea al venderle Regenta a un negocio.
 *
 * <p>Dar de alta un cliente no despliega nada. Una fila aqui, su configuracion,
 * su suscripcion y su primer administrador, y el negocio ya opera.
 *
 * <p>Vive SOLO en este servicio. Los demas no consultan esta tabla en cada
 * peticion: reciben negocio, plan y patron como claims del token.
 */
@Entity
@Table(name = "negocios")
public class Negocio {

    public static final String TRIAL = "TRIAL";
    public static final String ACTIVO = "ACTIVO";
    public static final String SUSPENDIDO = "SUSPENDIDO";
    public static final String CANCELADO = "CANCELADO";

    @Id
    private UUID id;

    @Column(name = "nombre_comercial", nullable = false, length = 150)
    private String nombreComercial;

    @Column(name = "razon_social", length = 200)
    private String razonSocial;

    @Column(name = "tipo_documento", nullable = false, length = 10)
    private String tipoDocumento;

    @Column(name = "numero_documento", nullable = false, length = 30)
    private String numeroDocumento;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "digito_verificacion", length = 1)
    private String digitoVerificacion;

    /**
     * Inmutable en la practica: cambiarlo despues de operar exige migrar datos
     * entre modelos distintos. El servicio lo bloquea si ya hubo movimiento.
     */
    @Column(name = "patron_operativo", nullable = false, updatable = false, length = 30)
    private String patronOperativo;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(nullable = false, length = 20)
    private String estado;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 2)
    private String pais;

    @Column(name = "zona_horaria", nullable = false, length = 50)
    private String zonaHoraria;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private String moneda;

    @Column(nullable = false, length = 5)
    private String idioma;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    @Version
    @Column(nullable = false)
    private long version;

    protected Negocio() {
    }

    private Negocio(UUID id, String nombreComercial, String razonSocial, String tipoDocumento,
            String numeroDocumento, String digitoVerificacion, String patronOperativo, UUID planId,
            String pais, String zonaHoraria, String moneda, String idioma) {
        this.id = id;
        this.nombreComercial = nombreComercial;
        this.razonSocial = razonSocial;
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
        this.digitoVerificacion = digitoVerificacion;
        this.patronOperativo = patronOperativo;
        this.planId = planId;
        this.estado = TRIAL;
        this.pais = pais;
        this.zonaHoraria = zonaHoraria;
        this.moneda = moneda;
        this.idioma = idioma;
    }

    /** Un negocio nace en TRIAL: paga despues de probar. */
    public static Negocio nuevo(String nombreComercial, String razonSocial, String tipoDocumento,
            String numeroDocumento, String digitoVerificacion, String patronOperativo, UUID planId,
            String pais, String zonaHoraria, String moneda, String idioma) {
        return new Negocio(UUID.randomUUID(), nombreComercial, razonSocial, tipoDocumento,
                numeroDocumento, digitoVerificacion, patronOperativo, planId, pais, zonaHoraria,
                moneda, idioma);
    }

    public UUID getId() {
        return id;
    }

    public String getNombreComercial() {
        return nombreComercial;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public String getDigitoVerificacion() {
        return digitoVerificacion;
    }

    public String getPatronOperativo() {
        return patronOperativo;
    }

    public UUID getPlanId() {
        return planId;
    }

    public String getEstado() {
        return estado;
    }

    public String getPais() {
        return pais;
    }

    public String getZonaHoraria() {
        return zonaHoraria;
    }

    public String getMoneda() {
        return moneda;
    }

    public String getIdioma() {
        return idioma;
    }

    public long getVersion() {
        return version;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }

    /** Con el negocio sin servicio, el gateway ni siquiera enruta. */
    public boolean recibeServicio() {
        return TRIAL.equals(estado) || ACTIVO.equals(estado);
    }

    public void activar() {
        this.estado = ACTIVO;
    }

    public void suspender() {
        this.estado = SUSPENDIDO;
    }

    public void cancelar() {
        this.estado = CANCELADO;
    }

    public void cambiarDePlan(UUID nuevoPlan) {
        this.planId = nuevoPlan;
    }

    public void renombrar(String nombreComercial, String razonSocial) {
        this.nombreComercial = nombreComercial;
        this.razonSocial = razonSocial;
    }

    public void corregirDocumento(String tipoDocumento, String numeroDocumento,
            String digitoVerificacion) {
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
        this.digitoVerificacion = digitoVerificacion;
    }
}
