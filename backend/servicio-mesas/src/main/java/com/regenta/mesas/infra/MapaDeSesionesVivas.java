package com.regenta.mesas.infra;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Qué mesa está en qué sesión viva ahora mismo (HU-083 criterio 4 y HU-084
 * criterio 1): la principal y las unidas, con cuándo se abrió y cuántos
 * comensales, para pintar el estado, el cronómetro y el nº de comensales en el
 * plano. Va por {@link JdbcTemplate} para no traer las entidades al pintar; la
 * RLS acota al negocio de la transacción.
 */
@Component
public class MapaDeSesionesVivas {

    private static final String SQL = """
            SELECT s.mesa_principal_id AS mesa_id, s.id AS sesion_id,
                   s.abierta_en AS abierta_en, s.num_comensales AS num_comensales
              FROM mesas.sesiones_mesa s
             WHERE s.estado IN ('ABIERTA','CUENTA_PEDIDA')
            UNION
            SELECT sm.mesa_id, s.id, s.abierta_en, s.num_comensales
              FROM mesas.sesion_mesas sm
              JOIN mesas.sesiones_mesa s ON s.id = sm.sesion_id
             WHERE s.estado IN ('ABIERTA','CUENTA_PEDIDA')
            """;

    private final JdbcTemplate jdbc;

    public MapaDeSesionesVivas(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** {@code mesaId -> sesión viva} de todas las mesas del negocio. */
    public Map<UUID, SesionEnMesa> porMesa() {
        Map<UUID, SesionEnMesa> mapa = new HashMap<>();
        jdbc.query(SQL, rs -> {
            OffsetDateTime abiertaEn = rs.getObject("abierta_en", OffsetDateTime.class);
            mapa.put(UUID.fromString(rs.getString("mesa_id")),
                    new SesionEnMesa(UUID.fromString(rs.getString("sesion_id")), abiertaEn,
                            rs.getInt("num_comensales")));
        });
        return mapa;
    }

    /** La sesión viva a la que pertenece una mesa, para el plano. */
    public record SesionEnMesa(UUID sesionId, OffsetDateTime abiertaEn, int numComensales) {
    }
}
