package com.regenta.facturacion.infra;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.regenta.facturacion.domain.EventoDeTransmision;

/**
 * El log de cada intercambio con la DIAN. Es evidencia legal: request y response
 * completos. La tabla está particionada por fecha con PK compuesta, así que se
 * escribe con {@link JdbcTemplate} y no con una entidad. Corre dentro de la
 * transacción del caso de uso, así la RLS lo acota al negocio.
 */
@Repository
public class TransmisionesLog {

    private final JdbcTemplate jdbc;

    public TransmisionesLog(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void registrar(UUID negocioId, UUID facturaId, EventoDeTransmision evento,
            String endpoint, String request, String response, Integer httpStatus,
            String codigoError, String mensaje, Integer duracionMs) {
        jdbc.update("""
                insert into facturacion.transmisiones
                    (id, negocio_id, factura_id, evento, endpoint, request, response,
                     http_status, codigo_error, mensaje, duracion_ms)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(), negocioId, facturaId, evento.name(), endpoint, request, response,
                httpStatus, codigoError, mensaje, duracionMs);
    }

    public List<Map<String, Object>> deLaFactura(UUID facturaId) {
        return jdbc.queryForList("""
                select evento, http_status, codigo_error, mensaje, duracion_ms, ocurrido_en
                from facturacion.transmisiones where factura_id = ? order by ocurrido_en
                """, facturaId);
    }
}
