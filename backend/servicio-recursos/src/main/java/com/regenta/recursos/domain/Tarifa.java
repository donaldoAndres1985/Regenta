package com.regenta.recursos.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
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
 * Una regla de precio de un recurso o tipo de recurso (HU-066): "Temporada
 * alta", "Fin de semana". Se acumulan sin ordenarlas ni borrarlas: la de mayor
 * {@code prioridad} que aplica a la noche gana. La vigencia, los días de la
 * semana y la estancia mínima acotan cuándo aplica.
 */
@Entity
@Table(name = "tarifas")
public class Tarifa {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "tipo_recurso_id", updatable = false)
    private UUID tipoRecursoId;

    @Column(name = "recurso_id", updatable = false)
    private UUID recursoId;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "unidad_tiempo", nullable = false, length = 20)
    private UnidadTiempo unidadTiempo;

    @Column(name = "precio_base", nullable = false)
    private BigDecimal precioBase;

    @Column(name = "precio_persona_adicional", nullable = false)
    private BigDecimal precioPersonaAdicional;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private String moneda;

    @Column(name = "impuesto_id")
    private UUID impuestoId;

    @Column(name = "vigente_desde")
    private LocalDate vigenteDesde;

    @Column(name = "vigente_hasta")
    private LocalDate vigenteHasta;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "dias_semana", nullable = false, columnDefinition = "smallint[]")
    private short[] diasSemana;

    @Column(name = "hora_desde")
    private LocalTime horaDesde;

    @Column(name = "hora_hasta")
    private LocalTime horaHasta;

    @Column(name = "estancia_minima", nullable = false)
    private int estanciaMinima;

    @Column(nullable = false)
    private short prioridad;

    @Column(nullable = false)
    private boolean activa;

    protected Tarifa() {
    }

    public static Tarifa crear(UUID negocioId, UUID tipoRecursoId, UUID recursoId, String nombre,
            UnidadTiempo unidadTiempo, BigDecimal precioBase, BigDecimal precioPersonaAdicional,
            LocalDate vigenteDesde, LocalDate vigenteHasta, List<Integer> diasSemana,
            LocalTime horaDesde, LocalTime horaHasta, int estanciaMinima, int prioridad) {
        Tarifa t = new Tarifa();
        t.id = UUID.randomUUID();
        t.negocioId = negocioId;
        t.tipoRecursoId = tipoRecursoId;
        t.recursoId = recursoId;
        t.moneda = "COP";
        t.activa = true;
        t.aplicar(nombre, unidadTiempo, precioBase, precioPersonaAdicional, vigenteDesde,
                vigenteHasta, diasSemana, horaDesde, horaHasta, estanciaMinima, prioridad);
        return t;
    }

    public void editar(String nombre, UnidadTiempo unidadTiempo, BigDecimal precioBase,
            BigDecimal precioPersonaAdicional, LocalDate vigenteDesde, LocalDate vigenteHasta,
            List<Integer> diasSemana, LocalTime horaDesde, LocalTime horaHasta, int estanciaMinima,
            int prioridad) {
        aplicar(nombre, unidadTiempo, precioBase, precioPersonaAdicional, vigenteDesde,
                vigenteHasta, diasSemana, horaDesde, horaHasta, estanciaMinima, prioridad);
    }

    private void aplicar(String nombre, UnidadTiempo unidadTiempo, BigDecimal precioBase,
            BigDecimal precioPersonaAdicional, LocalDate vigenteDesde, LocalDate vigenteHasta,
            List<Integer> diasSemana, LocalTime horaDesde, LocalTime horaHasta, int estanciaMinima,
            int prioridad) {
        this.nombre = nombre;
        this.unidadTiempo = unidadTiempo == null ? UnidadTiempo.NOCHE : unidadTiempo;
        this.precioBase = precioBase == null || precioBase.signum() < 0 ? BigDecimal.ZERO
                : precioBase;
        this.precioPersonaAdicional = precioPersonaAdicional == null
                || precioPersonaAdicional.signum() < 0 ? BigDecimal.ZERO : precioPersonaAdicional;
        this.vigenteDesde = vigenteDesde;
        this.vigenteHasta = vigenteHasta;
        this.diasSemana = normalizarDias(diasSemana);
        this.horaDesde = horaDesde;
        this.horaHasta = horaHasta;
        this.estanciaMinima = Math.max(1, estanciaMinima);
        this.prioridad = (short) prioridad;
    }

    public void desactivar() {
        this.activa = false;
    }

    /** Es una tarifa de un recurso concreto, no de todo el tipo. */
    public boolean esPuntual() {
        return recursoId != null;
    }

    /**
     * Si esta tarifa aplica a una noche de una estancia (HU-066 criterios 2, 3 y
     * 5): tiene que estar activa, la fecha dentro de la vigencia, el día de la
     * semana permitido y la estancia alcanzar la mínima.
     */
    public boolean aplicaEn(LocalDate noche, int nochesEstancia) {
        if (!activa) {
            return false;
        }
        if (vigenteDesde != null && noche.isBefore(vigenteDesde)) {
            return false;
        }
        if (vigenteHasta != null && noche.isAfter(vigenteHasta)) {
            return false;
        }
        if (nochesEstancia < estanciaMinima) {
            return false;
        }
        int dia = noche.getDayOfWeek().getValue(); // 1=lunes .. 7=domingo
        for (short d : diasSemana) {
            if (d == dia) {
                return true;
            }
        }
        return false;
    }

    /** El precio de una noche con esta tarifa para {@code personas}. */
    public BigDecimal precioDeNoche(int personas) {
        int adicionales = Math.max(0, personas - 1);
        return precioBase.add(precioPersonaAdicional.multiply(BigDecimal.valueOf(adicionales)));
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

    public UUID getTipoRecursoId() {
        return tipoRecursoId;
    }

    public UUID getRecursoId() {
        return recursoId;
    }

    public String getNombre() {
        return nombre;
    }

    public UnidadTiempo getUnidadTiempo() {
        return unidadTiempo;
    }

    public BigDecimal getPrecioBase() {
        return precioBase;
    }

    public BigDecimal getPrecioPersonaAdicional() {
        return precioPersonaAdicional;
    }

    public String getMoneda() {
        return moneda;
    }

    public LocalDate getVigenteDesde() {
        return vigenteDesde;
    }

    public LocalDate getVigenteHasta() {
        return vigenteHasta;
    }

    public List<Integer> getDiasSemana() {
        List<Integer> out = new java.util.ArrayList<>(diasSemana.length);
        for (short d : diasSemana) {
            out.add((int) d);
        }
        return out;
    }

    public int getEstanciaMinima() {
        return estanciaMinima;
    }

    public int getPrioridad() {
        return prioridad;
    }

    public boolean isActiva() {
        return activa;
    }
}
