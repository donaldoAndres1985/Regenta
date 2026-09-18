package com.regenta.clientes.domain;

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
 * Un cliente del negocio: sus datos fiscales y de contacto (HU-021). La
 * identidad la da el par {@code (tipo_documento, numero_documento)}, unico por
 * negocio salvo para {@code SIN_IDENTIFICAR} —el «consumidor final», que se
 * repite cuanto haga falta.
 *
 * <p>{@code nombre_display} lo calcula la base: {@code razon_social} si es
 * persona juridica, o {@code nombres + apellidos} si es natural. Aqui se lee,
 * no se escribe.
 */
@Entity
@Table(name = "clientes")
public class Cliente {

    /** Nombre que toma un cliente sin identificar cuando no se da ninguno. */
    public static final String CONSUMIDOR_FINAL = "Consumidor final";

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_persona", nullable = false, length = 20)
    private TipoPersona tipoPersona;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 20)
    private TipoDocumento tipoDocumento;

    @Column(name = "numero_documento", length = 30)
    private String numeroDocumento;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "digito_verificacion", length = 1)
    private String digitoVerificacion;

    @Column(length = 120)
    private String nombres;

    @Column(length = 120)
    private String apellidos;

    @Column(name = "razon_social", length = 200)
    private String razonSocial;

    @Column(name = "nombre_display", length = 200, insertable = false, updatable = false)
    private String nombreDisplay;

    @Column(length = 150)
    private String email;

    @Column(length = 30)
    private String telefono;

    @Column(name = "telefono_alterno", length = 30)
    private String telefonoAlterno;

    @Column(length = 40)
    private String segmento;

    @Column(name = "credito_habilitado", nullable = false)
    private boolean creditoHabilitado;

    @Column(name = "cupo_credito", nullable = false)
    private BigDecimal cupoCredito;

    @Column(name = "dias_credito", nullable = false)
    private short diasCredito;

    /** Proyección de cartera: lo que el cliente debe. La mantiene HU-022. */
    @Column(name = "saldo_pendiente", nullable = false)
    private BigDecimal saldoPendiente;

    @Column(columnDefinition = "text")
    private String notas;

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

    @Column(name = "eliminado_en")
    private OffsetDateTime eliminadoEn;

    @Version
    @Column(nullable = false)
    private long version;

    protected Cliente() {
    }

    /**
     * Alta de un cliente ya normalizado por el servicio (persona, documento y
     * nombre resueltos). Aqui solo se asignan los valores.
     */
    /**
     * HU-114 criterio 5: el cliente que se creo sin conexion ya tiene id —se
     * lo puso el dispositivo— y sube con ese. Solo vale antes de guardarlo:
     * despues, cambiarle el id seria crear otro cliente.
     */
    public void conElIdDelDispositivo(UUID id) {
        if (id != null) {
            this.id = id;
        }
    }

    public static Cliente crear(UUID negocioId, UUID creadoPor, TipoPersona tipoPersona,
            TipoDocumento tipoDocumento, String numeroDocumento, String digitoVerificacion,
            String nombres, String apellidos, String razonSocial, String email, String telefono,
            String telefonoAlterno, String segmento, String notas) {
        Cliente cliente = new Cliente();
        cliente.id = UUID.randomUUID();
        cliente.negocioId = negocioId;
        cliente.creadoPor = creadoPor;
        cliente.tipoPersona = tipoPersona;
        cliente.tipoDocumento = tipoDocumento;
        cliente.numeroDocumento = numeroDocumento;
        cliente.digitoVerificacion = digitoVerificacion;
        cliente.nombres = nombres;
        cliente.apellidos = apellidos;
        cliente.razonSocial = razonSocial;
        cliente.email = email;
        cliente.telefono = telefono;
        cliente.telefonoAlterno = telefonoAlterno;
        cliente.segmento = segmento;
        cliente.notas = notas;
        cliente.creditoHabilitado = false;
        cliente.cupoCredito = BigDecimal.ZERO;
        cliente.diasCredito = 0;
        cliente.saldoPendiente = BigDecimal.ZERO;
        cliente.activo = true;
        return cliente;
    }

    /** HU-022: el administrador fija (o quita) el cupo de crédito del cliente. */
    public void configurarCredito(boolean habilitado, BigDecimal cupo, int diasCredito) {
        this.creditoHabilitado = habilitado;
        this.cupoCredito = cupo == null || cupo.signum() < 0 ? BigDecimal.ZERO : cupo;
        this.diasCredito = (short) Math.max(0, diasCredito);
    }

    /** Suma (o resta, con signo negativo) a la cartera. Nunca por debajo de 0. */
    public void ajustarSaldoPendiente(BigDecimal delta) {
        this.saldoPendiente = this.saldoPendiente.add(delta).max(BigDecimal.ZERO);
    }

    /** Cambia los datos editables. Persona y documento no se tocan aqui. */
    public void editar(String nombres, String apellidos, String razonSocial, String email,
            String telefono, String telefonoAlterno, String segmento, String notas,
            UUID actualizadoPor) {
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.razonSocial = razonSocial;
        this.email = email;
        this.telefono = telefono;
        this.telefonoAlterno = telefonoAlterno;
        this.segmento = segmento;
        this.notas = notas;
        this.actualizadoPor = actualizadoPor;
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

    public TipoPersona getTipoPersona() {
        return tipoPersona;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public String getDigitoVerificacion() {
        return digitoVerificacion;
    }

    public String getNombres() {
        return nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    /**
     * Lo mismo que calcula la columna generada de la base, para poder devolver
     * el nombre recien creado sin ir a releer la fila: {@code razon_social}, o
     * {@code nombres + apellidos} recortado.
     */
    public String getNombreDisplay() {
        if (nombreDisplay != null) {
            return nombreDisplay;
        }
        if (razonSocial != null) {
            return razonSocial;
        }
        return ((nombres == null ? "" : nombres) + " " + (apellidos == null ? "" : apellidos))
                .trim();
    }

    public String getEmail() {
        return email;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getTelefonoAlterno() {
        return telefonoAlterno;
    }

    public String getSegmento() {
        return segmento;
    }

    public boolean isCreditoHabilitado() {
        return creditoHabilitado;
    }

    public BigDecimal getCupoCredito() {
        return cupoCredito;
    }

    public short getDiasCredito() {
        return diasCredito;
    }

    /** Lo que aún cabe a crédito: cupo menos lo que ya debe, nunca negativo. */
    public BigDecimal getCupoDisponible() {
        return cupoCredito.subtract(saldoPendiente).max(BigDecimal.ZERO);
    }

    public BigDecimal getSaldoPendiente() {
        return saldoPendiente;
    }

    public String getNotas() {
        return notas;
    }

    public boolean isActivo() {
        return activo;
    }

    public long getVersion() {
        return version;
    }
}
