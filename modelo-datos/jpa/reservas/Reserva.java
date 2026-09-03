package com.regenta.reservas.dominio;

import com.regenta.comun.dominio.EntidadTenant;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

/**
 * Agregado raíz del patrón Reserva.
 *
 * Diferencia esencial con Venta: aquí la transacción ocupa un INTERVALO DE
 * TIEMPO, y dos reservas del mismo recurso no pueden solaparse.
 *
 * Esa regla NO se valida en Java. Un "¿está libre?" seguido de un INSERT
 * tiene una ventana de carrera: dos peticiones concurrentes leen "libre" y
 * ambas insertan. La garantía real es el constraint de exclusión GiST del
 * DDL (ex_reserva_solape), que es atómico. Java solo traduce la excepción.
 */
@Entity
@Table(name = "reservas", schema = "reservas")
public class Reserva extends EntidadTenant {

    @Column(name = "numero", nullable = false, length = 30) private String numero;
    @Column(name = "sucursal_id") private UUID sucursalId;
    @Column(name = "cliente_id")  private UUID clienteId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cliente_snapshot", columnDefinition = "jsonb")
    private Map<String, Object> clienteSnapshot;

    @Column(name = "tipo_recurso_id", nullable = false) private UUID tipoRecursoId;
    @Column(name = "recurso_id") private UUID recursoId;

    /**
     * TSTZRANGE no tiene tipo JPA estándar. Se mapea con un UserType propio
     * (PeriodoTstzRangeType). La alternativa —dos columnas inicio/fin— haría
     * imposible el constraint de exclusión, que es todo el punto.
     */
    @Column(name = "periodo", nullable = false, columnDefinition = "tstzrange")
    // @Type(PeriodoTstzRangeType.class)
    private Periodo periodo;

    /** Calculado en el servicio con la zona horaria del negocio: la columna
     *  no puede ser GENERATED porque el cast tstz→date no es inmutable. */
    @Column(name = "noches", nullable = false) private int noches = 1;

    @Column(name = "num_personas", nullable = false) private short numPersonas = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoReserva estado = EstadoReserva.PENDIENTE;

    @Column(name = "total",    nullable = false, precision = 16, scale = 4) private BigDecimal total = BigDecimal.ZERO;
    @Column(name = "anticipo", nullable = false, precision = 16, scale = 4) private BigDecimal anticipo = BigDecimal.ZERO;
    @Column(name = "saldo",    nullable = false, precision = 16, scale = 4) private BigDecimal saldo = BigDecimal.ZERO;

    public enum EstadoReserva {
        PENDIENTE, CONFIRMADA, CHECK_IN, CHECK_OUT, CANCELADA, NO_SHOW, EXPIRADA
    }

    /** Value object del intervalo, con semántica [inicio, fin): el check-out
     *  de las 11:00 y el check-in de las 11:00 NO se solapan. */
    public record Periodo(OffsetDateTime inicio, OffsetDateTime fin) {
        public Periodo {
            if (!fin.isAfter(inicio))
                throw new IllegalArgumentException("El fin del periodo debe ser posterior al inicio");
        }
        public long noches() { return Math.max(1, ChronoUnit.DAYS.between(inicio.toLocalDate(), fin.toLocalDate())); }
        public boolean solapaCon(Periodo o) { return inicio.isBefore(o.fin) && o.inicio.isBefore(fin); }
    }

    public void asignarRecurso(UUID recursoId) {
        if (estado == EstadoReserva.CANCELADA || estado == EstadoReserva.CHECK_OUT)
            throw new IllegalStateException("No se reasigna recurso a una reserva cerrada");
        this.recursoId = recursoId;   // el EXCLUDE decide si es posible
    }

    public void confirmar() {
        if (estado != EstadoReserva.PENDIENTE)
            throw new IllegalStateException("Solo se confirma una reserva pendiente");
        this.estado = EstadoReserva.CONFIRMADA;
    }

    /**
     * Cancelar libera el solape: el EXCLUDE del DDL solo aplica a
     * PENDIENTE / CONFIRMADA / CHECK_IN, así que el recurso queda disponible
     * de inmediato sin borrar el registro histórico.
     */
    public BigDecimal cancelar(int horasDeAntelacion, BigDecimal penalizacionPct) {
        if (estado == EstadoReserva.CHECK_OUT)
            throw new IllegalStateException("Una estancia finalizada no se cancela");
        this.estado = EstadoReserva.CANCELADA;
        return horasDeAntelacion >= 0
            ? total.multiply(penalizacionPct).movePointLeft(2)
            : BigDecimal.ZERO;
    }
}
