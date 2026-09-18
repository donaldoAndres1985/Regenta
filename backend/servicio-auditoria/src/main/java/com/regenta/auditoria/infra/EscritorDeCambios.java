package com.regenta.auditoria.infra;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Inserta en {@code cambios_servidor}. Por JDBC plano, no JPA: particionada
 * por rango de {@code ocurrido_en} con clave primaria compuesta
 * {@code (id, ocurrido_en)}, el mismo caso que {@code EscritorDeAuditoria}.
 */
@Component
public class EscritorDeCambios {

    private final JdbcTemplate jdbc;

    public EscritorDeCambios(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insertar(UUID negocioId, String entidadTipo, UUID entidadId, String operacion,
            String payload, OffsetDateTime ocurridoEn) {
        jdbc.update("""
                INSERT INTO auditoria.cambios_servidor (negocio_id, entidad_tipo, entidad_id, operacion,
                    version, payload, ocurrido_en)
                VALUES (?, ?, ?, ?, 0, ?::jsonb, ?)
                """,
                negocioId, entidadTipo, entidadId, operacion, payload, ocurridoEn);
    }
}
