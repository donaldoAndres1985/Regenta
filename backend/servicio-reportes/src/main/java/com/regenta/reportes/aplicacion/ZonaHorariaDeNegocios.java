package com.regenta.reportes.aplicacion;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * La zona horaria de cada negocio, cacheada localmente (HU-097 criterio 3).
 * Servicio-reportes no consulta la base de servicio-usuarios —el dato
 * maestro— así que la toma del evento {@code negocio_creado} y la guarda
 * aquí para no fallar cuando le llega el primer documento a agregar.
 */
@Component
public class ZonaHorariaDeNegocios {

    /** Mismo valor por defecto que usa servicio-usuarios cuando el alta no especifica zona. */
    public static final String POR_DEFECTO = "America/Bogota";

    private final JdbcTemplate jdbc;

    public ZonaHorariaDeNegocios(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public void guardar(UUID negocioId, String zonaHoraria) {
        jdbc.update("""
                INSERT INTO reportes.config_negocio (negocio_id, zona_horaria)
                VALUES (?, ?)
                ON CONFLICT (negocio_id) DO UPDATE SET
                    zona_horaria = EXCLUDED.zona_horaria, actualizado_en = now()
                """,
                negocioId, zonaHoraria == null || zonaHoraria.isBlank() ? POR_DEFECTO : zonaHoraria);
    }

    @Transactional(readOnly = true)
    public String de(UUID negocioId) {
        String zona = jdbc.query("select zona_horaria from reportes.config_negocio where negocio_id = ?",
                rs -> rs.next() ? rs.getString(1) : null, negocioId);
        return zona == null ? POR_DEFECTO : zona;
    }
}
