package com.regenta.mesas.infra;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Si una mesa tiene una sesión viva (HU-081 criterio 4). Va por
 * {@link JdbcTemplate} porque la entidad de {@code sesiones_mesa} la trae HU-082;
 * aquí solo hace falta el conteo para no borrar una mesa que está ocupada. La
 * RLS acota todo al negocio de la transacción.
 *
 * <p>Cuenta tanto la mesa principal como una mesa unida al grupo (HU-083).
 */
@Component
public class ConteoDeSesionesAbiertas {

    private static final String VIVAS = "('ABIERTA','CUENTA_PEDIDA')";

    private final JdbcTemplate jdbc;

    public ConteoDeSesionesAbiertas(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean laMesaTieneSesionViva(UUID mesaId) {
        Long n = jdbc.queryForObject(
                "SELECT count(*) FROM mesas.sesiones_mesa s "
                        + "WHERE s.estado IN " + VIVAS + " AND ("
                        + "  s.mesa_principal_id = ? "
                        + "  OR EXISTS (SELECT 1 FROM mesas.sesion_mesas sm "
                        + "             WHERE sm.sesion_id = s.id AND sm.mesa_id = ?))",
                Long.class, mesaId, mesaId);
        return n != null && n > 0;
    }
}
