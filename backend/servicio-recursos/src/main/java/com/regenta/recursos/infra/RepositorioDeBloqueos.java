package com.regenta.recursos.infra;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.regenta.recursos.domain.BloqueoDeRecurso;
import com.regenta.recursos.domain.MotivoBloqueo;

/**
 * Bloqueos de recurso (HU-067). Va por {@link JdbcTemplate} y no por JPA porque
 * la columna {@code periodo} es un {@code tstzrange}: aquí se arma con
 * {@code tstzrange(?, ?, '[)')} y se lee con {@code lower()}/{@code upper()}.
 *
 * <p>El {@code EXCLUDE USING gist (recurso_id WITH =, periodo WITH &&)} de la
 * tabla es quien rechaza dos bloqueos solapados del mismo recurso; un insert que
 * choca sale como {@code DataIntegrityViolationException}. La RLS acota todo al
 * negocio de la transacción, así que los métodos se llaman siempre dentro de una.
 */
@Repository
public class RepositorioDeBloqueos {

    private static final String COLUMNAS = "id, negocio_id, recurso_id, "
            + "lower(periodo) AS desde, upper(periodo) AS hasta, motivo, detalle, "
            + "usuario_id, creado_en";

    private static final RowMapper<BloqueoDeRecurso> A_BLOQUEO = (rs, fila) -> new BloqueoDeRecurso(
            rs.getObject("id", UUID.class),
            rs.getObject("negocio_id", UUID.class),
            rs.getObject("recurso_id", UUID.class),
            rs.getObject("desde", OffsetDateTime.class),
            rs.getObject("hasta", OffsetDateTime.class),
            MotivoBloqueo.valueOf(rs.getString("motivo")),
            rs.getString("detalle"),
            rs.getObject("usuario_id", UUID.class),
            rs.getObject("creado_en", OffsetDateTime.class));

    private final JdbcTemplate jdbc;

    public RepositorioDeBloqueos(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Inserta el bloqueo. Lanza {@code DataIntegrityViolationException} si se solapa con otro. */
    public void insertar(BloqueoDeRecurso bloqueo) {
        jdbc.update(
                "INSERT INTO recursos.bloqueos_recurso "
                        + "(id, negocio_id, recurso_id, periodo, motivo, detalle, usuario_id) "
                        + "VALUES (?, ?, ?, tstzrange(?, ?, '[)'), ?, ?, ?)",
                bloqueo.getId(), bloqueo.getNegocioId(), bloqueo.getRecursoId(),
                bloqueo.getDesde(), bloqueo.getHasta(), bloqueo.getMotivo().name(),
                bloqueo.getDetalle(), bloqueo.getUsuarioId());
    }

    public Optional<BloqueoDeRecurso> buscar(UUID id) {
        return jdbc.query("SELECT " + COLUMNAS + " FROM recursos.bloqueos_recurso WHERE id = ?",
                A_BLOQUEO, id).stream().findFirst();
    }

    public List<BloqueoDeRecurso> porRecurso(UUID recursoId) {
        return jdbc.query("SELECT " + COLUMNAS + " FROM recursos.bloqueos_recurso "
                + "WHERE recurso_id = ? ORDER BY lower(periodo)", A_BLOQUEO, recursoId);
    }

    /** Los bloqueos del recurso que pisan {@code [desde, hasta)}. */
    public List<BloqueoDeRecurso> queSolapan(UUID recursoId, OffsetDateTime desde,
            OffsetDateTime hasta) {
        return jdbc.query("SELECT " + COLUMNAS + " FROM recursos.bloqueos_recurso "
                + "WHERE recurso_id = ? AND periodo && tstzrange(?, ?, '[)') "
                + "ORDER BY lower(periodo)", A_BLOQUEO, recursoId, desde, hasta);
    }

    public void eliminar(UUID id) {
        jdbc.update("DELETE FROM recursos.bloqueos_recurso WHERE id = ?", id);
    }
}
