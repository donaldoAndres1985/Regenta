package com.regenta.alertas.domain;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
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
 * Una alerta generada por una regla (HU-092). La {@code huella} es única por
 * negocio ({@code uq_alerta_huella}): la misma condición repetida no crea otra
 * fila. Dentro de la ventana de silencio de la regla no se toca; pasada la
 * ventana y ya resuelta, se reabre.
 */
@Entity
@Table(name = "alertas")
public class Alerta {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "sucursal_id")
    private UUID sucursalId;

    @Column(name = "regla_id")
    private UUID reglaId;

    @Column(name = "tipo_codigo", nullable = false, length = 40)
    private String tipoCodigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Severidad severidad;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, columnDefinition = "text")
    private String mensaje;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> datos;

    @Column(name = "entidad_tipo", length = 40)
    private String entidadTipo;

    @Column(name = "entidad_id")
    private UUID entidadId;

    @Column(name = "ruta_app", length = 200)
    private String rutaApp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAlerta estado;

    @Column(nullable = false, length = 120)
    private String huella;

    @Column(name = "generada_en", nullable = false)
    private OffsetDateTime generadaEn;

    @Column(name = "vista_en")
    private OffsetDateTime vistaEn;

    @Column(name = "resuelta_en")
    private OffsetDateTime resueltaEn;

    @Column(name = "resuelta_por")
    private UUID resueltaPor;

    @Column(name = "expira_en")
    private OffsetDateTime expiraEn;

    protected Alerta() {
    }

    public static Alerta generar(UUID negocioId, ReglaAlerta regla, String tipoCodigo,
            Severidad severidad, String titulo, String mensaje, Map<String, Object> datos,
            String entidadTipo, UUID entidadId, String rutaApp, String huella) {
        Alerta a = new Alerta();
        a.id = UUID.randomUUID();
        a.negocioId = negocioId;
        a.reglaId = regla == null ? null : regla.getId();
        a.sucursalId = regla == null ? null : regla.getSucursalId();
        a.tipoCodigo = tipoCodigo;
        a.severidad = severidad;
        a.titulo = recortar(titulo, 150);
        a.mensaje = mensaje;
        a.datos = datos == null ? Map.of() : datos;
        a.entidadTipo = entidadTipo;
        a.entidadId = entidadId;
        a.rutaApp = rutaApp;
        a.estado = EstadoAlerta.NUEVA;
        a.huella = huella;
        a.generadaEn = OffsetDateTime.now();
        return a;
    }

    /** Pasada la ventana y ya resuelta/descartada: vuelve a NUEVA con fecha de ahora. */
    public void reabrir(String titulo, String mensaje, Map<String, Object> datos) {
        this.estado = EstadoAlerta.NUEVA;
        this.titulo = recortar(titulo, 150);
        this.mensaje = mensaje;
        this.datos = datos == null ? Map.of() : datos;
        this.generadaEn = OffsetDateTime.now();
        this.vistaEn = null;
        this.resueltaEn = null;
        this.resueltaPor = null;
    }

    public void marcarVista() {
        if (estado == EstadoAlerta.NUEVA) {
            this.estado = EstadoAlerta.VISTA;
            this.vistaEn = OffsetDateTime.now();
        }
    }

    public void resolver(UUID usuarioId) {
        this.estado = EstadoAlerta.RESUELTA;
        this.resueltaEn = OffsetDateTime.now();
        this.resueltaPor = usuarioId;
    }

    public boolean estaActiva() {
        return estado.estaActiva();
    }

    public boolean dentroDeVentanaDeSilencio(int silenciarHoras, OffsetDateTime ahora) {
        if (silenciarHoras <= 0) {
            return false;
        }
        return ChronoUnit.HOURS.between(generadaEn, ahora) < silenciarHoras;
    }

    private static String recortar(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getReglaId() {
        return reglaId;
    }

    public String getTipoCodigo() {
        return tipoCodigo;
    }

    public Severidad getSeveridad() {
        return severidad;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public Map<String, Object> getDatos() {
        return datos;
    }

    public String getEntidadTipo() {
        return entidadTipo;
    }

    public UUID getEntidadId() {
        return entidadId;
    }

    public String getRutaApp() {
        return rutaApp;
    }

    public EstadoAlerta getEstado() {
        return estado;
    }

    public String getHuella() {
        return huella;
    }

    public OffsetDateTime getGeneradaEn() {
        return generadaEn;
    }

    public OffsetDateTime getResueltaEn() {
        return resueltaEn;
    }

    public UUID getResueltaPor() {
        return resueltaPor;
    }
}
