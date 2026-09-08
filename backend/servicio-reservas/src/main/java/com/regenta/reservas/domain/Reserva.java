package com.regenta.reservas.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import com.regenta.comun.errores.ReglaDeNegocioException;

/**
 * Una reserva de un recurso para un periodo (HU-070). El anti-overbooking no
 * vive aquí: lo garantiza el {@code EXCLUDE USING gist} sobre {@code periodo} en
 * la tabla. Esta clase valida lo que sí es responsabilidad del dominio —que el
 * periodo tenga sentido— y guarda el desglose de dinero que trae la cotización.
 *
 * <p>No es una entidad JPA: {@code periodo} es un {@code tstzrange} y se maneja
 * con {@code JdbcTemplate} (igual que los bloqueos de recurso).
 */
public final class Reserva {

    private final UUID id;
    private final UUID negocioId;
    private final UUID sucursalId;
    private final String numero;
    private final UUID clienteId;
    private final UUID tipoRecursoId;
    private final UUID recursoId;
    private final OffsetDateTime desde;
    private final OffsetDateTime hasta;
    private final int noches;
    private final int numAdultos;
    private final int numNinos;
    private final EstadoReserva estado;
    private final CanalReserva canal;
    private final UUID tarifaId;
    private final UUID politicaCancelacionId;
    private final BigDecimal subtotal;
    private final BigDecimal total;
    private final BigDecimal anticipoRequerido;
    private final BigDecimal saldo;
    private final String moneda;
    private final UUID usuarioId;
    private final String notas;
    private final OffsetDateTime creadoEn;
    private final long version;

    private Reserva(UUID id, UUID negocioId, UUID sucursalId, String numero, UUID clienteId,
            UUID tipoRecursoId, UUID recursoId, OffsetDateTime desde, OffsetDateTime hasta,
            int noches, int numAdultos, int numNinos, EstadoReserva estado, CanalReserva canal,
            UUID tarifaId, UUID politicaCancelacionId, BigDecimal subtotal, BigDecimal total,
            BigDecimal anticipoRequerido, BigDecimal saldo, String moneda, UUID usuarioId,
            String notas, OffsetDateTime creadoEn, long version) {
        this.id = id;
        this.negocioId = negocioId;
        this.sucursalId = sucursalId;
        this.numero = numero;
        this.clienteId = clienteId;
        this.tipoRecursoId = tipoRecursoId;
        this.recursoId = recursoId;
        this.desde = desde;
        this.hasta = hasta;
        this.noches = noches;
        this.numAdultos = numAdultos;
        this.numNinos = numNinos;
        this.estado = estado;
        this.canal = canal;
        this.tarifaId = tarifaId;
        this.politicaCancelacionId = politicaCancelacionId;
        this.subtotal = subtotal;
        this.total = total;
        this.anticipoRequerido = anticipoRequerido;
        this.saldo = saldo;
        this.moneda = moneda;
        this.usuarioId = usuarioId;
        this.notas = notas;
        this.creadoEn = creadoEn;
        this.version = version;
    }

    /** Cuántas noches cubre {@code [desde, hasta)}, contando por fecha y con mínimo 1. */
    public static int nochesEntre(OffsetDateTime desde, OffsetDateTime hasta) {
        long dias = ChronoUnit.DAYS.between(desde.toLocalDate(), hasta.toLocalDate());
        return (int) Math.max(1, dias);
    }

    /**
     * Una reserva nueva, sin persistir. Nace {@code PENDIENTE}; la confirmación y
     * la cancelación son de HU-071.
     */
    public static Reserva nueva(UUID negocioId, String numero, UUID tipoRecursoId, UUID recursoId,
            OffsetDateTime desde, OffsetDateTime hasta, int numAdultos, int numNinos,
            CanalReserva canal, UUID clienteId, UUID sucursalId, UUID tarifaId,
            UUID politicaCancelacionId, BigDecimal total, BigDecimal anticipoRequerido,
            String moneda, UUID usuarioId, String notas) {
        if (desde == null || hasta == null) {
            throw new ReglaDeNegocioException("La reserva necesita fecha de entrada y de salida");
        }
        if (!hasta.isAfter(desde)) {
            throw new ReglaDeNegocioException(
                    "La salida de la reserva debe ser posterior a la entrada");
        }
        if (recursoId == null) {
            throw new ReglaDeNegocioException("La reserva necesita un recurso asignado");
        }
        int adultos = Math.max(1, numAdultos);
        int ninos = Math.max(0, numNinos);
        BigDecimal totalSeguro = total == null || total.signum() < 0 ? BigDecimal.ZERO : total;
        BigDecimal anticipoSeguro = anticipoRequerido == null || anticipoRequerido.signum() < 0
                ? BigDecimal.ZERO
                : anticipoRequerido.min(totalSeguro);
        return new Reserva(UUID.randomUUID(), negocioId, sucursalId, numero, clienteId,
                tipoRecursoId, recursoId, desde, hasta, nochesEntre(desde, hasta), adultos, ninos,
                EstadoReserva.PENDIENTE, canal == null ? CanalReserva.MOSTRADOR : canal, tarifaId,
                politicaCancelacionId, totalSeguro.setScale(4, RoundingMode.HALF_UP),
                totalSeguro.setScale(4, RoundingMode.HALF_UP),
                anticipoSeguro.setScale(4, RoundingMode.HALF_UP),
                totalSeguro.subtract(anticipoSeguro).setScale(4, RoundingMode.HALF_UP),
                moneda == null || moneda.isBlank() ? "COP" : moneda, usuarioId,
                notas == null || notas.isBlank() ? null : notas.trim(), null, 0L);
    }

    /** Reconstruye una reserva leída de la base. */
    public static Reserva rehidratar(UUID id, UUID negocioId, UUID sucursalId, String numero,
            UUID clienteId, UUID tipoRecursoId, UUID recursoId, OffsetDateTime desde,
            OffsetDateTime hasta, int noches, int numAdultos, int numNinos, EstadoReserva estado,
            CanalReserva canal, UUID tarifaId, UUID politicaCancelacionId, BigDecimal subtotal,
            BigDecimal total, BigDecimal anticipoRequerido, BigDecimal saldo, String moneda,
            UUID usuarioId, String notas, OffsetDateTime creadoEn, long version) {
        return new Reserva(id, negocioId, sucursalId, numero, clienteId, tipoRecursoId, recursoId,
                desde, hasta, noches, numAdultos, numNinos, estado, canal, tarifaId,
                politicaCancelacionId, subtotal, total, anticipoRequerido, saldo, moneda, usuarioId,
                notas, creadoEn, version);
    }

    public int numPersonas() {
        return numAdultos + numNinos;
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

    public String getNumero() {
        return numero;
    }

    public UUID getClienteId() {
        return clienteId;
    }

    public UUID getTipoRecursoId() {
        return tipoRecursoId;
    }

    public UUID getRecursoId() {
        return recursoId;
    }

    public OffsetDateTime getDesde() {
        return desde;
    }

    public OffsetDateTime getHasta() {
        return hasta;
    }

    public int getNoches() {
        return noches;
    }

    public int getNumAdultos() {
        return numAdultos;
    }

    public int getNumNinos() {
        return numNinos;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public CanalReserva getCanal() {
        return canal;
    }

    public UUID getTarifaId() {
        return tarifaId;
    }

    public UUID getPoliticaCancelacionId() {
        return politicaCancelacionId;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public BigDecimal getAnticipoRequerido() {
        return anticipoRequerido;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public String getMoneda() {
        return moneda;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public String getNotas() {
        return notas;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }

    public long getVersion() {
        return version;
    }
}
