package com.regenta.auditoria.infra;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Inserta en {@code eventos_auditoria}. Por JDBC plano, no JPA: la tabla está
 * particionada por rango de {@code ocurrido_en} con clave primaria compuesta
 * {@code (id, ocurrido_en)}, el mismo caso que {@code EscritorDeHechos} en
 * servicio-reportes.
 */
@Component
public class EscritorDeAuditoria {

    private final JdbcTemplate jdbc;

    public EscritorDeAuditoria(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insertar(UUID id, UUID negocioId, UUID usuarioId, String servicio, String entidadTipo,
            UUID entidadId, String accion, String datosDespues, String traceId, OffsetDateTime ocurridoEn) {
        jdbc.update("""
                INSERT INTO auditoria.eventos_auditoria (id, negocio_id, usuario_id, servicio, entidad_tipo,
                    entidad_id, accion, datos_despues, trace_id, resultado, ocurrido_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, 'OK', ?)
                """,
                id, negocioId, usuarioId, servicio, entidadTipo, entidadId, accion, datosDespues, traceId,
                ocurridoEn);
    }
}
