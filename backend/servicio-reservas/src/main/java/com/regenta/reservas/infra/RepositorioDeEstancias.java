package com.regenta.reservas.infra;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.regenta.reservas.domain.Estancia;
import com.regenta.reservas.domain.EstadoEstancia;

/** {@code estancias} por {@link JdbcTemplate}. La RLS lo acota al negocio de la transacción. */
@Repository
public class RepositorioDeEstancias {

    private static final String COLUMNAS = "id, negocio_id, reserva_id, recurso_asignado_id, "
            + "check_in_en, check_in_usuario_id, check_out_previsto, estado, consumo_total, "
            + "deposito, observaciones_entrada, version";

    private static final RowMapper<Estancia> A_ESTANCIA = (rs, fila) -> Estancia.rehidratar(
            rs.getObject("id", UUID.class),
            rs.getObject("negocio_id", UUID.class),
            rs.getObject("reserva_id", UUID.class),
            rs.getObject("recurso_asignado_id", UUID.class),
            rs.getObject("check_in_en", OffsetDateTime.class),
            rs.getObject("check_in_usuario_id", UUID.class),
            rs.getObject("check_out_previsto", OffsetDateTime.class),
            EstadoEstancia.valueOf(rs.getString("estado")),
            rs.getBigDecimal("consumo_total"),
            rs.getBigDecimal("deposito"),
            rs.getString("observaciones_entrada"),
            rs.getLong("version"));

    private final JdbcTemplate jdbc;

    public RepositorioDeEstancias(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void crear(Estancia e) {
        jdbc.update(
                "INSERT INTO reservas.estancias "
                        + "(id, negocio_id, reserva_id, recurso_asignado_id, check_in_en, "
                        + " check_in_usuario_id, check_out_previsto, estado, deposito, "
                        + " observaciones_entrada) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                e.getId(), e.getNegocioId(), e.getReservaId(), e.getRecursoAsignadoId(),
                e.getCheckInEn(), e.getCheckInUsuarioId(), e.getCheckOutPrevisto(),
                e.getEstado().name(), e.getDeposito(), e.getObservacionesEntrada());
    }

    public Optional<Estancia> porReserva(UUID reservaId) {
        return jdbc.query("SELECT " + COLUMNAS + " FROM reservas.estancias WHERE reserva_id = ?",
                A_ESTANCIA, reservaId).stream().findFirst();
    }

    public Optional<Estancia> buscar(UUID id) {
        return jdbc.query("SELECT " + COLUMNAS + " FROM reservas.estancias WHERE id = ?",
                A_ESTANCIA, id).stream().findFirst();
    }
}
