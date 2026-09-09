package com.regenta.reservas.infra;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.regenta.reservas.domain.CanalReserva;
import com.regenta.reservas.domain.EstadoReserva;
import com.regenta.reservas.domain.Reserva;

/**
 * Persistencia de {@link Reserva} (HU-070/HU-071). Va por {@link JdbcTemplate}
 * porque {@code reservas.periodo} es un {@code tstzrange}: se escribe con
 * {@code tstzrange(?, ?, '[)')} y se lee con {@code lower()}/{@code upper()}. El
 * {@code EXCLUDE USING gist} de la tabla rechaza los solapes; un insert que
 * choca sale como {@code DataIntegrityViolationException}. La RLS acota todo al
 * negocio de la transacción.
 */
@Repository
public class RepositorioDeReservas {

    /** Nombre del constraint de exclusión, para reconocer el overbooking. */
    public static final String CONSTRAINT_SOLAPE = "ex_reserva_solape";

    private static final String COLUMNAS = "id, negocio_id, sucursal_id, numero, cliente_id, "
            + "tipo_recurso_id, recurso_id, lower(periodo) AS desde, upper(periodo) AS hasta, "
            + "noches, num_adultos, num_ninos, estado, canal, tarifa_id, politica_cancelacion_id, "
            + "subtotal, total, anticipo, saldo, penalizacion, moneda, usuario_id, notas, "
            + "confirmada_en, cancelada_en, motivo_cancelacion, creado_en, version";

    private static final RowMapper<Reserva> A_RESERVA = (rs, fila) -> Reserva.rehidratar(
            rs.getObject("id", UUID.class),
            rs.getObject("negocio_id", UUID.class),
            rs.getObject("sucursal_id", UUID.class),
            rs.getString("numero"),
            rs.getObject("cliente_id", UUID.class),
            rs.getObject("tipo_recurso_id", UUID.class),
            rs.getObject("recurso_id", UUID.class),
            rs.getObject("desde", OffsetDateTime.class),
            rs.getObject("hasta", OffsetDateTime.class),
            rs.getInt("noches"),
            rs.getInt("num_adultos"),
            rs.getInt("num_ninos"),
            EstadoReserva.valueOf(rs.getString("estado")),
            CanalReserva.valueOf(rs.getString("canal")),
            rs.getObject("tarifa_id", UUID.class),
            rs.getObject("politica_cancelacion_id", UUID.class),
            rs.getBigDecimal("subtotal"),
            rs.getBigDecimal("total"),
            rs.getBigDecimal("anticipo"),
            rs.getBigDecimal("saldo"),
            rs.getBigDecimal("penalizacion"),
            rs.getString("moneda"),
            rs.getObject("usuario_id", UUID.class),
            rs.getString("notas"),
            rs.getObject("confirmada_en", OffsetDateTime.class),
            rs.getObject("cancelada_en", OffsetDateTime.class),
            rs.getString("motivo_cancelacion"),
            rs.getObject("creado_en", OffsetDateTime.class),
            rs.getLong("version"));

    private final JdbcTemplate jdbc;

    public RepositorioDeReservas(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Inserta la reserva. Lanza {@code DataIntegrityViolationException} si el recurso ya está tomado. */
    public void insertar(Reserva r) {
        jdbc.update(
                "INSERT INTO reservas.reservas "
                        + "(id, negocio_id, sucursal_id, numero, cliente_id, tipo_recurso_id, "
                        + " recurso_id, periodo, noches, num_personas, num_adultos, num_ninos, "
                        + " estado, canal, tarifa_id, politica_cancelacion_id, subtotal, total, "
                        + " anticipo, saldo, moneda, usuario_id, notas) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, tstzrange(?, ?, '[)'), ?, ?, ?, ?, "
                        + " ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                r.getId(), r.getNegocioId(), r.getSucursalId(), r.getNumero(), r.getClienteId(),
                r.getTipoRecursoId(), r.getRecursoId(), r.getDesde(), r.getHasta(), r.getNoches(),
                r.numPersonas(), r.getNumAdultos(), r.getNumNinos(), r.getEstado().name(),
                r.getCanal().name(), r.getTarifaId(), r.getPoliticaCancelacionId(), r.getSubtotal(),
                r.getTotal(), r.getAnticipoRequerido(), r.getSaldo(), r.getMoneda(),
                r.getUsuarioId(), r.getNotas());
    }

    /**
     * Guarda una transición de estado con control de versión optimista. Devuelve
     * false si la fila cambió por debajo (otra transacción se adelantó).
     */
    public boolean actualizarEstado(Reserva r, long versionEsperada) {
        int filas = jdbc.update(
                "UPDATE reservas.reservas SET estado = ?, penalizacion = ?, confirmada_en = ?, "
                        + " cancelada_en = ?, motivo_cancelacion = ?, actualizado_en = now(), "
                        + " version = version + 1 "
                        + "WHERE id = ? AND version = ?",
                r.getEstado().name(), r.getPenalizacion(), r.getConfirmadaEn(), r.getCanceladaEn(),
                r.getMotivoCancelacion(), r.getId(), versionEsperada);
        return filas == 1;
    }

    /** Deja el cambio de estado en el historial de auditoría (HU-071 criterio 5). */
    public void registrarEvento(UUID negocioId, UUID reservaId, EstadoReserva anterior,
            EstadoReserva nuevo, UUID usuarioId, String detalle) {
        jdbc.update(
                "INSERT INTO reservas.reserva_eventos "
                        + "(id, negocio_id, reserva_id, estado_anterior, estado_nuevo, usuario_id, detalle) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), negocioId, reservaId, anterior == null ? null : anterior.name(),
                nuevo.name(), usuarioId, detalle);
    }

    public Optional<Reserva> buscar(UUID id) {
        return jdbc.query("SELECT " + COLUMNAS + " FROM reservas.reservas WHERE id = ?",
                A_RESERVA, id).stream().findFirst();
    }
}
