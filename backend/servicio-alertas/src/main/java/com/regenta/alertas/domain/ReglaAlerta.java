package com.regenta.alertas.domain;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import jakarta.persistence.Version;

/**
 * Una regla de alerta de un negocio (HU-092): un tipo del catálogo + una
 * condición declarativa + severidad + canales + destinatarios. "Alertar
 * vencimientos en droguería" es una fila de esta tabla con tipo
 * {@code VENCIMIENTO_LOTE}, no código nuevo.
 */
@Entity
@Table(name = "reglas_alerta")
public class ReglaAlerta {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "sucursal_id")
    private UUID sucursalId;

    @Column(name = "tipo_codigo", nullable = false, length = 40)
    private String tipoCodigo;

    @Column(nullable = false, length = 80)
    private String nombre;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> condicion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Severidad severidad;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    private String[] canales;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "destinatarios_roles", nullable = false, columnDefinition = "text[]")
    private String[] destinatariosRoles;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "destinatarios_usuarios", nullable = false, columnDefinition = "uuid[]")
    private UUID[] destinatariosUsuarios;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FrecuenciaAlerta frecuencia;

    @Column(name = "hora_envio")
    private LocalTime horaEnvio;

    @Column(name = "silenciar_horas", nullable = false)
    private short silenciarHoras;

    @Column(nullable = false)
    private boolean activa;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Version
    @Column(nullable = false)
    private long version;

    protected ReglaAlerta() {
    }

    public static ReglaAlerta crear(UUID negocioId, UUID sucursalId, String tipoCodigo,
            String nombre, Map<String, Object> condicion, Severidad severidad,
            List<String> canales, List<String> destinatariosRoles,
            List<UUID> destinatariosUsuarios, FrecuenciaAlerta frecuencia, LocalTime horaEnvio,
            int silenciarHoras) {
        ReglaAlerta r = new ReglaAlerta();
        r.id = UUID.randomUUID();
        r.negocioId = negocioId;
        r.sucursalId = sucursalId;
        r.tipoCodigo = tipoCodigo;
        r.aplicar(nombre, condicion, severidad, canales, destinatariosRoles,
                destinatariosUsuarios, frecuencia, horaEnvio, silenciarHoras);
        r.activa = true;
        return r;
    }

    public void editar(String nombre, Map<String, Object> condicion, Severidad severidad,
            List<String> canales, List<String> destinatariosRoles,
            List<UUID> destinatariosUsuarios, FrecuenciaAlerta frecuencia, LocalTime horaEnvio,
            int silenciarHoras) {
        aplicar(nombre, condicion, severidad, canales, destinatariosRoles,
                destinatariosUsuarios, frecuencia, horaEnvio, silenciarHoras);
    }

    private void aplicar(String nombre, Map<String, Object> condicion, Severidad severidad,
            List<String> canales, List<String> destinatariosRoles,
            List<UUID> destinatariosUsuarios, FrecuenciaAlerta frecuencia, LocalTime horaEnvio,
            int silenciarHoras) {
        this.nombre = nombre;
        this.condicion = condicion == null ? Map.of() : condicion;
        this.severidad = severidad;
        this.canales = (canales == null || canales.isEmpty() ? List.of("IN_APP") : canales)
                .toArray(String[]::new);
        this.destinatariosRoles =
                (destinatariosRoles == null ? List.<String>of() : destinatariosRoles)
                        .toArray(String[]::new);
        this.destinatariosUsuarios =
                (destinatariosUsuarios == null ? List.<UUID>of() : destinatariosUsuarios)
                        .toArray(UUID[]::new);
        this.frecuencia = frecuencia == null ? FrecuenciaAlerta.INMEDIATA : frecuencia;
        this.horaEnvio = horaEnvio;
        this.silenciarHoras = (short) Math.max(0, silenciarHoras);
    }

    public void activar() {
        this.activa = true;
    }

    public void desactivar() {
        this.activa = false;
    }

    /** Criterios 2 y 4: solo dispara si está activa y la condición coincide. */
    public boolean disparaCon(Map<String, Object> campos) {
        return activa && EvaluadorDeCondicion.coincide(condicion, campos);
    }

    /** Los usuarios objetivo, resueltos los roles con el directorio dado. */
    public Set<UUID> destinatarios(java.util.function.Function<String, List<UUID>> porRol) {
        Set<UUID> objetivo = new LinkedHashSet<>();
        for (String rol : destinatariosRoles) {
            objetivo.addAll(porRol.apply(rol));
        }
        for (UUID u : destinatariosUsuarios) {
            objetivo.add(u);
        }
        return objetivo;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getSucursalId() {
        return sucursalId;
    }

    public String getTipoCodigo() {
        return tipoCodigo;
    }

    public String getNombre() {
        return nombre;
    }

    public Map<String, Object> getCondicion() {
        return condicion;
    }

    public Severidad getSeveridad() {
        return severidad;
    }

    public List<String> getCanales() {
        return List.of(canales);
    }

    public List<String> getDestinatariosRoles() {
        return List.of(destinatariosRoles);
    }

    public List<UUID> getDestinatariosUsuarios() {
        return List.of(destinatariosUsuarios);
    }

    public FrecuenciaAlerta getFrecuencia() {
        return frecuencia;
    }

    public LocalTime getHoraEnvio() {
        return horaEnvio;
    }

    public int getSilenciarHoras() {
        return silenciarHoras;
    }

    public boolean isActiva() {
        return activa;
    }
}
