package com.regenta.reportes.aplicacion;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;

/**
 * Las métricas del patrón Reserva (HU-099 criterios 1 y 3): ocupación, ADR y
 * RevPAR por día. Un hotel no se mide con las de una tienda.
 *
 * <p>Se leen de {@code ocupacion_diaria}, que ya viene noche por noche: esto
 * no recorre {@code hechos_reserva}.
 */
@Service
public class MetricasDeReserva {

    private final JdbcTemplate jdbc;

    public MetricasDeReserva(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Criterio 1. El rango incluye los dos extremos. */
    @Transactional(readOnly = true)
    @RequierePermiso("REPORTES_REPORTE_VER")
    public List<OcupacionDelDia> ocupacion(LocalDate desde, LocalDate hasta) {
        PatronDelNegocio.exigir("RESERVA", "ocupación");
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new ReglaDeNegocioException("El rango de fechas no es válido");
        }
        UUID negocioId = ContextoDeNegocio.negocioActual();
        return jdbc.query("""
                SELECT o.fecha, o.tipo_recurso_id, o.recursos_totales, o.recursos_ocupados,
                       o.ocupacion_pct, o.ingreso_alojamiento, o.adr, o.revpar,
                       (SELECT r.tipo_recurso_nombre FROM reportes.dim_recurso r
                         WHERE r.negocio_id = o.negocio_id AND r.tipo_recurso_id = o.tipo_recurso_id
                         LIMIT 1) AS tipo_recurso
                  FROM reportes.ocupacion_diaria o
                 WHERE o.negocio_id = ? AND o.fecha BETWEEN ? AND ?
                 ORDER BY o.fecha, tipo_recurso
                """,
                (rs, fila) -> new OcupacionDelDia(rs.getDate("fecha").toLocalDate(),
                        rs.getObject("tipo_recurso_id", UUID.class), rs.getString("tipo_recurso"),
                        rs.getInt("recursos_totales"), rs.getInt("recursos_ocupados"),
                        rs.getBigDecimal("ocupacion_pct"), rs.getBigDecimal("ingreso_alojamiento"),
                        rs.getBigDecimal("adr"), rs.getBigDecimal("revpar")),
                negocioId, desde, hasta);
    }
}
