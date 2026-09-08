package com.regenta.recursos.domain;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Un tipo de recurso reservable (HU-064): habitación doble, cancha F5,
 * consultorio. Es el espejo de {@code categorias} en Inventario — la misma
 * técnica, otro patrón. Define la unidad de tiempo, la granularidad y los
 * buffers de limpieza/preparación que se descuentan entre reservas.
 */
@Entity
@Table(name = "tipos_recurso")
public class TipoDeRecurso {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(columnDefinition = "text")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "unidad_tiempo", nullable = false, length = 20)
    private UnidadTiempo unidadTiempo;

    @Column(name = "duracion_minima_min", nullable = false)
    private int duracionMinimaMin;

    @Column(name = "incremento_min", nullable = false)
    private int incrementoMin;

    @Column(name = "capacidad_default", nullable = false)
    private short capacidadDefault;

    @Column(name = "permite_overbooking", nullable = false)
    private boolean permiteOverbooking;

    @Column(name = "buffer_antes_min", nullable = false)
    private int bufferAntesMin;

    @Column(name = "buffer_despues_min", nullable = false)
    private int bufferDespuesMin;

    @Column(nullable = false)
    private boolean activo;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Version
    @Column(nullable = false)
    private long version;

    protected TipoDeRecurso() {
    }

    public static TipoDeRecurso crear(UUID negocioId, String nombre, String descripcion,
            UnidadTiempo unidadTiempo, int duracionMinimaMin, int incrementoMin,
            int capacidadDefault, boolean permiteOverbooking, int bufferAntesMin,
            int bufferDespuesMin) {
        TipoDeRecurso t = new TipoDeRecurso();
        t.id = UUID.randomUUID();
        t.negocioId = negocioId;
        t.activo = true;
        t.aplicar(nombre, descripcion, unidadTiempo, duracionMinimaMin, incrementoMin,
                capacidadDefault, permiteOverbooking, bufferAntesMin, bufferDespuesMin);
        return t;
    }

    public void editar(String nombre, String descripcion, UnidadTiempo unidadTiempo,
            int duracionMinimaMin, int incrementoMin, int capacidadDefault,
            boolean permiteOverbooking, int bufferAntesMin, int bufferDespuesMin) {
        aplicar(nombre, descripcion, unidadTiempo, duracionMinimaMin, incrementoMin,
                capacidadDefault, permiteOverbooking, bufferAntesMin, bufferDespuesMin);
    }

    private void aplicar(String nombre, String descripcion, UnidadTiempo unidadTiempo,
            int duracionMinimaMin, int incrementoMin, int capacidadDefault,
            boolean permiteOverbooking, int bufferAntesMin, int bufferDespuesMin) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.unidadTiempo = unidadTiempo == null ? UnidadTiempo.NOCHE : unidadTiempo;
        this.duracionMinimaMin = Math.max(1, duracionMinimaMin);
        this.incrementoMin = Math.max(1, incrementoMin);
        this.capacidadDefault = (short) Math.max(1, capacidadDefault);
        this.permiteOverbooking = permiteOverbooking;
        this.bufferAntesMin = Math.max(0, bufferAntesMin);
        this.bufferDespuesMin = Math.max(0, bufferDespuesMin);
    }

    public void desactivar() {
        this.activo = false;
    }

    /**
     * Criterio 3: la franja que una reserva bloquea de verdad es la reservada
     * más los buffers de preparación antes y de limpieza después. La
     * disponibilidad (HU-069) resta esta ventana, no solo la reserva.
     */
    public OffsetDateTime[] ventanaConBuffer(OffsetDateTime inicio, OffsetDateTime fin) {
        return new OffsetDateTime[] {
                inicio.minus(Duration.ofMinutes(bufferAntesMin)),
                fin.plus(Duration.ofMinutes(bufferDespuesMin)),
        };
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public UnidadTiempo getUnidadTiempo() {
        return unidadTiempo;
    }

    public int getDuracionMinimaMin() {
        return duracionMinimaMin;
    }

    public int getIncrementoMin() {
        return incrementoMin;
    }

    public int getCapacidadDefault() {
        return capacidadDefault;
    }

    public boolean isPermiteOverbooking() {
        return permiteOverbooking;
    }

    public int getBufferAntesMin() {
        return bufferAntesMin;
    }

    public int getBufferDespuesMin() {
        return bufferDespuesMin;
    }

    public boolean isActivo() {
        return activo;
    }
}
