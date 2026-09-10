package com.regenta.mesas.infra;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Qué mesa está en qué sesión viva ahora mismo (HU-083 criterio 4): la principal
 * y las unidas. Va por {@link JdbcTemplate} para no traer las entidades al pintar
 * el plano; la RLS acota al negocio de la transacción.
 */
@Component
public class MapaDeSesionesVivas {

    private static final String SQL = """
            SELECT s.mesa_principal_id AS mesa_id, s.id AS sesion_id
              FROM mesas.sesiones_mesa s
             WHERE s.estado IN ('ABIERTA','CUENTA_PEDIDA')
            UNION
            SELECT sm.mesa_id, sm.sesion_id
              FROM mesas.sesion_mesas sm
              JOIN mesas.sesiones_mesa s ON s.id = sm.sesion_id
             WHERE s.estado IN ('ABIERTA','CUENTA_PEDIDA')
            """;

    private final JdbcTemplate jdbc;

    public MapaDeSesionesVivas(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** {@code mesaId -> sesionId} de todas las mesas con sesión viva del negocio. */
    public Map<UUID, UUID> porMesa() {
        Map<UUID, UUID> mapa = new HashMap<>();
        jdbc.query(SQL, rs -> {
            mapa.put(UUID.fromString(rs.getString("mesa_id")),
                    UUID.fromString(rs.getString("sesion_id")));
        });
        return mapa;
    }
}
