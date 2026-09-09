package com.regenta.menu.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Una carta del restaurante (HU-076): "Carta principal", "Desayunos", "Happy
 * hour". Cada una vale para una franja horaria, unos días de la semana y un
 * rango de fechas; fuera de eso no aparece entre las disponibles (criterio 1).
 */
@Entity
@Table(name = "cartas")
public class Carta {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "sucursal_id")
    private UUID sucursalId;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(columnDefinition = "text")
    private String descripcion;

    @Column(name = "hora_desde")
    private LocalTime horaDesde;

    @Column(name = "hora_hasta")
    private LocalTime horaHasta;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "dias_semana", nullable = false, columnDefinition = "smallint[]")
    private short[] diasSemana;

    @Column(name = "vigente_desde")
    private LocalDate vigenteDesde;

    @Column(name = "vigente_hasta")
    private LocalDate vigenteHasta;

    @Column(name = "es_default", nullable = false)
    private boolean esDefault;

    @Column(nullable = false)
    private boolean activa;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Version
    @Column(nullable = false)
    private long version;

    protected Carta() {
    }

    public static Carta crear(UUID negocioId, UUID sucursalId, String nombre, String descripcion,
            LocalTime horaDesde, LocalTime horaHasta, List<Integer> diasSemana,
            LocalDate vigenteDesde, LocalDate vigenteHasta, boolean esDefault) {
        Carta c = new Carta();
        c.id = UUID.randomUUID();
        c.negocioId = negocioId;
        c.sucursalId = sucursalId;
        c.activa = true;
        c.aplicar(nombre, descripcion, horaDesde, horaHasta, diasSemana, vigenteDesde, vigenteHasta);
        c.esDefault = esDefault;
        return c;
    }

    public void editar(String nombre, String descripcion, LocalTime horaDesde, LocalTime horaHasta,
            List<Integer> diasSemana, LocalDate vigenteDesde, LocalDate vigenteHasta) {
        aplicar(nombre, descripcion, horaDesde, horaHasta, diasSemana, vigenteDesde, vigenteHasta);
    }

    private void aplicar(String nombre, String descripcion, LocalTime horaDesde, LocalTime horaHasta,
            List<Integer> diasSemana, LocalDate vigenteDesde, LocalDate vigenteHasta) {
        this.nombre = nombre;
        this.descripcion = descripcion == null || descripcion.isBlank() ? null : descripcion.trim();
        this.horaDesde = horaDesde;
        this.horaHasta = horaHasta;
        this.diasSemana = normalizarDias(diasSemana);
        this.vigenteDesde = vigenteDesde;
        this.vigenteHasta = vigenteHasta;
    }

    public void activar() {
        this.activa = true;
    }

    public void desactivar() {
        this.activa = false;
        this.esDefault = false;
    }

    public void marcarPorDefecto() {
        this.esDefault = true;
    }

    public void quitarPorDefecto() {
        this.esDefault = false;
    }

    /**
     * Si esta carta está disponible en ese instante (HU-076 criterio 1): activa,
     * dentro de la vigencia de fechas, el día de la semana permitido y la hora
     * dentro de la franja. Una franja que cruza la medianoche (22:00–02:00) se
     * entiende bien.
     */
    public boolean disponibleEn(LocalDateTime momento) {
        if (!activa) {
            return false;
        }
        LocalDate dia = momento.toLocalDate();
        if (vigenteDesde != null && dia.isBefore(vigenteDesde)) {
            return false;
        }
        if (vigenteHasta != null && dia.isAfter(vigenteHasta)) {
            return false;
        }
        int diaSemana = dia.getDayOfWeek().getValue(); // 1=lunes .. 7=domingo
        boolean diaPermitido = false;
        for (short d : diasSemana) {
            if (d == diaSemana) {
                diaPermitido = true;
                break;
            }
        }
        if (!diaPermitido) {
            return false;
        }
        return dentroDeLaFranja(momento.toLocalTime());
    }

    private boolean dentroDeLaFranja(LocalTime hora) {
        if (horaDesde == null && horaHasta == null) {
            return true;
        }
        LocalTime desde = horaDesde == null ? LocalTime.MIN : horaDesde;
        LocalTime hasta = horaHasta == null ? LocalTime.MAX : horaHasta;
        if (!desde.isAfter(hasta)) {
            return !hora.isBefore(desde) && !hora.isAfter(hasta);
        }
        // Cruza la medianoche.
        return !hora.isBefore(desde) || !hora.isAfter(hasta);
    }

    private static short[] normalizarDias(List<Integer> dias) {
        if (dias == null || dias.isEmpty()) {
            return new short[] {1, 2, 3, 4, 5, 6, 7};
        }
        return dias.stream().filter(d -> d >= 1 && d <= 7).distinct().sorted()
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toList(),
                        lista -> {
                            short[] out = new short[lista.size()];
                            for (int i = 0; i < lista.size(); i++) {
                                out[i] = lista.get(i).shortValue();
                            }
                            return out;
                        }));
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

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public LocalTime getHoraDesde() {
        return horaDesde;
    }

    public LocalTime getHoraHasta() {
        return horaHasta;
    }

    public List<Integer> getDiasSemana() {
        List<Integer> out = new java.util.ArrayList<>(diasSemana.length);
        for (short d : diasSemana) {
            out.add((int) d);
        }
        return out;
    }

    public LocalDate getVigenteDesde() {
        return vigenteDesde;
    }

    public LocalDate getVigenteHasta() {
        return vigenteHasta;
    }

    public boolean isEsDefault() {
        return esDefault;
    }

    public boolean isActiva() {
        return activa;
    }

    public long getVersion() {
        return version;
    }
}
