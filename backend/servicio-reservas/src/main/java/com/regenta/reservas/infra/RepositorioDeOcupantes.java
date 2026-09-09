package com.regenta.reservas.infra;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.regenta.reservas.domain.Ocupante;

/** {@code ocupantes} por {@link JdbcTemplate}. La RLS lo acota al negocio de la transacción. */
@Repository
public class RepositorioDeOcupantes {

    private static final RowMapper<Ocupante> A_OCUPANTE = (rs, fila) -> Ocupante.rehidratar(
            rs.getObject("id", UUID.class),
            rs.getObject("negocio_id", UUID.class),
            rs.getObject("reserva_id", UUID.class),
            rs.getBoolean("es_titular"),
            rs.getString("nombres"),
            rs.getString("apellidos"),
            rs.getString("tipo_documento"),
            rs.getString("numero_documento"),
            rs.getString("nacionalidad"),
            rs.getObject("fecha_nacimiento", LocalDate.class),
            rs.getString("telefono"),
            rs.getString("email"));

    private final JdbcTemplate jdbc;

    public RepositorioDeOcupantes(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void agregar(Ocupante o) {
        jdbc.update(
                "INSERT INTO reservas.ocupantes "
                        + "(id, negocio_id, reserva_id, es_titular, nombres, apellidos, "
                        + " tipo_documento, numero_documento, nacionalidad, fecha_nacimiento, "
                        + " telefono, email) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                o.getId(), o.getNegocioId(), o.getReservaId(), o.esTitular(), o.getNombres(),
                o.getApellidos(), o.getTipoDocumento(), o.getNumeroDocumento(), o.getNacionalidad(),
                o.getFechaNacimiento(), o.getTelefono(), o.getEmail());
    }

    public List<Ocupante> porReserva(UUID reservaId) {
        return jdbc.query(
                "SELECT id, negocio_id, reserva_id, es_titular, nombres, apellidos, tipo_documento, "
                        + " numero_documento, nacionalidad, fecha_nacimiento, telefono, email "
                        + "FROM reservas.ocupantes WHERE reserva_id = ? "
                        + "ORDER BY es_titular DESC, apellidos, nombres",
                A_OCUPANTE, reservaId);
    }

    public boolean tieneTitular(UUID reservaId) {
        Integer n = jdbc.queryForObject(
                "SELECT count(*) FROM reservas.ocupantes WHERE reserva_id = ? AND es_titular = true",
                Integer.class, reservaId);
        return n != null && n > 0;
    }
}
