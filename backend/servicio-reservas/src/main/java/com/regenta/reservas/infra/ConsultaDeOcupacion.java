package com.regenta.reservas.infra;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.regenta.reservas.aplicacion.ReservaEnCalendario;
import com.regenta.reservas.domain.VentanaOcupada;

/**
 * Lee de {@code reservas} los tramos que un recurso tiene tomados en un periodo
 * (HU-069). Va por {@link JdbcTemplate} porque {@code reservas.periodo} es un
 * {@code tstzrange}: se compara con {@code && tstzrange(?, ?, '[)')} y se lee con
 * {@code lower()}/{@code upper()}. Solo cuentan las reservas con
 * {@code recurso_id} asignado y en un estado que ocupa (criterio 2). La RLS
 * acota todo al negocio de la transacción.
 */
@Repository
public class ConsultaDeOcupacion {

    private static final RowMapper<VentanaOcupada> A_VENTANA = (rs, fila) -> new VentanaOcupada(
            rs.getObject("recurso_id", UUID.class),
            rs.getObject("desde", OffsetDateTime.class),
            rs.getObject("hasta", OffsetDateTime.class));

    private static final String BASE =
            "SELECT recurso_id, lower(periodo) AS desde, upper(periodo) AS hasta "
                    + "FROM reservas.reservas "
                    + "WHERE recurso_id IS NOT NULL "
                    + "  AND estado IN ('PENDIENTE','CONFIRMADA','CHECK_IN') "
                    + "  AND periodo && tstzrange(?, ?, '[)')";

    private final JdbcTemplate jdbc;

    public ConsultaDeOcupacion(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Tramos ocupados que se solapan con {@code [desde, hasta)}. Se pide ya
     * ensanchado por el buffer máximo; el filtro fino por buffer del tipo lo
     * hace el servicio.
     */
    public List<VentanaOcupada> ventanasOcupadas(UUID negocioId, OffsetDateTime desde,
            OffsetDateTime hasta, UUID tipoRecursoId) {
        if (tipoRecursoId == null) {
            return jdbc.query(BASE, A_VENTANA, desde, hasta);
        }
        return jdbc.query(BASE + " AND tipo_recurso_id = ?", A_VENTANA, desde, hasta,
                tipoRecursoId);
    }

    private static final RowMapper<ReservaEnCalendario> A_BARRA = (rs, fila) ->
            new ReservaEnCalendario(
                    rs.getObject("id", UUID.class),
                    rs.getObject("recurso_id", UUID.class),
                    rs.getString("numero"),
                    rs.getString("estado"),
                    rs.getObject("desde", OffsetDateTime.class),
                    rs.getObject("hasta", OffsetDateTime.class));

    private static final String CALENDARIO =
            "SELECT id, recurso_id, numero, estado, lower(periodo) AS desde, upper(periodo) AS hasta "
                    + "FROM reservas.reservas "
                    + "WHERE recurso_id IS NOT NULL "
                    + "  AND estado IN ('PENDIENTE','CONFIRMADA','CHECK_IN','CHECK_OUT') "
                    + "  AND periodo && tstzrange(?, ?, '[)')";

    /** Las reservas con recurso asignado que caen en {@code [desde, hasta)}, para el calendario (HU-075). */
    public List<ReservaEnCalendario> reservasEnCalendario(UUID negocioId, OffsetDateTime desde,
            OffsetDateTime hasta, UUID tipoRecursoId) {
        if (tipoRecursoId == null) {
            return jdbc.query(CALENDARIO + " ORDER BY lower(periodo)", A_BARRA, desde, hasta);
        }
        return jdbc.query(CALENDARIO + " AND tipo_recurso_id = ? ORDER BY lower(periodo)", A_BARRA,
                desde, hasta, tipoRecursoId);
    }
}
