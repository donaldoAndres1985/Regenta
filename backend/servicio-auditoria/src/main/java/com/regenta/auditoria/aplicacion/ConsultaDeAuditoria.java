package com.regenta.auditoria.aplicacion;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;

/** Consultas de la bitácora de auditoría. HU-101 criterio 4. */
@Service
public class ConsultaDeAuditoria {

    private final JdbcTemplate jdbc;

    public ConsultaDeAuditoria(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Criterio 4: reconstruye todo lo que hizo un request, aunque cruzara varios servicios. */
    @Transactional(readOnly = true)
    @RequierePermiso("AUDITORIA_BITACORA_VER")
    public List<EventoDeAuditoria> porTraceId(String traceId) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        return jdbc.query("""
                SELECT id, usuario_id, servicio, entidad_tipo, entidad_id, accion, trace_id, resultado,
                    ocurrido_en
                FROM auditoria.eventos_auditoria
                WHERE negocio_id = ? AND trace_id = ?
                ORDER BY ocurrido_en
                """, this::fila, negocioId, traceId);
    }

    private EventoDeAuditoria fila(ResultSet rs, int i) throws SQLException {
        return new EventoDeAuditoria(UUID.fromString(rs.getString("id")),
                rs.getString("usuario_id") == null ? null : UUID.fromString(rs.getString("usuario_id")),
                rs.getString("servicio"), rs.getString("entidad_tipo"),
                rs.getString("entidad_id") == null ? null : UUID.fromString(rs.getString("entidad_id")),
                rs.getString("accion"), rs.getString("trace_id"), rs.getString("resultado"),
                rs.getObject("ocurrido_en", OffsetDateTime.class));
    }
}
