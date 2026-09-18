package com.regenta.reportes.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final ZonaHorariaDeNegocios zonas;

    public MetricasDeReserva(JdbcTemplate jdbc, ZonaHorariaDeNegocios zonas) {
        this.jdbc = jdbc;
        this.zonas = zonas;
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
        Map<String, Map<String, Integer>> motivos = motivosDeBloqueo(negocioId, desde, hasta);
        return jdbc.query("""
                SELECT o.fecha, o.tipo_recurso_id, o.recursos_totales, o.recursos_ocupados,
                       o.recursos_bloqueados, o.ocupacion_pct, o.ingreso_alojamiento, o.adr, o.revpar,
                       (SELECT r.tipo_recurso_nombre FROM reportes.dim_recurso r
                         WHERE r.negocio_id = o.negocio_id AND r.tipo_recurso_id = o.tipo_recurso_id
                         LIMIT 1) AS tipo_recurso
                  FROM reportes.ocupacion_diaria o
                 WHERE o.negocio_id = ? AND o.fecha BETWEEN ? AND ?
                 ORDER BY o.fecha, tipo_recurso
                """,
                (rs, fila) -> {
                    LocalDate fecha = rs.getDate("fecha").toLocalDate();
                    UUID tipoRecursoId = rs.getObject("tipo_recurso_id", UUID.class);
                    return new OcupacionDelDia(fecha, tipoRecursoId, rs.getString("tipo_recurso"),
                            rs.getInt("recursos_totales"), rs.getInt("recursos_ocupados"),
                            rs.getInt("recursos_bloqueados"),
                            motivos.getOrDefault(clave(fecha, tipoRecursoId), Map.of()),
                            rs.getBigDecimal("ocupacion_pct"), rs.getBigDecimal("ingreso_alojamiento"),
                            rs.getBigDecimal("adr"), rs.getBigDecimal("revpar"));
                },
                negocioId, desde, hasta);
    }

    /**
     * HU-136 criterio 3: por qué estuvieron fuera de servicio. Va en una
     * consulta aparte y no como subconsulta agregada porque son dos granos
     * distintos —la ocupación es por tipo y noche, el motivo es por recurso— y
     * juntarlos en SQL obliga a un JSON que después hay que volver a parsear.
     */
    private Map<String, Map<String, Integer>> motivosDeBloqueo(UUID negocioId, LocalDate desde,
            LocalDate hasta) {
        Map<String, Map<String, Integer>> porDiaYTipo = new LinkedHashMap<>();
        jdbc.query("""
                SELECT fecha, tipo_recurso_id, motivo, count(*) AS cuantos
                  FROM reportes.bloqueos_recurso_dia
                 WHERE negocio_id = ? AND fecha BETWEEN ? AND ?
                 GROUP BY fecha, tipo_recurso_id, motivo
                """,
                rs -> {
                    porDiaYTipo.computeIfAbsent(
                            clave(rs.getDate("fecha").toLocalDate(),
                                    rs.getObject("tipo_recurso_id", UUID.class)),
                            k -> new LinkedHashMap<>())
                            .put(rs.getString("motivo"), rs.getInt("cuantos"));
                },
                negocioId, desde, hasta);
        return porDiaYTipo;
    }

    private static String clave(LocalDate fecha, UUID tipoRecursoId) {
        return fecha + "|" + tipoRecursoId;
    }

    /**
     * Criterio 3 de HU-135: la tasa de cancelación y la de no-show, junto a la
     * penalización cobrada. {@code totalReservas} son las que llegaron a un
     * estado final —finalizada, cancelada o no-show— en el período: una
     * reserva que sigue confirmada no entra en ninguna tasa todavía.
     */
    @Transactional(readOnly = true)
    @RequierePermiso("REPORTES_REPORTE_VER")
    public CancelacionesDelPeriodo cancelaciones(LocalDate desde, LocalDate hasta) {
        PatronDelNegocio.exigir("RESERVA", "cancelaciones");
        UUID negocioId = ContextoDeNegocio.negocioActual();
        OffsetDateTime[] rango = rango(negocioId, desde, hasta);

        return jdbc.queryForObject("""
                SELECT count(*) FILTER (WHERE estado_final IN ('FINALIZADA', 'CANCELADA', 'NO_SHOW'))
                           AS total,
                       count(*) FILTER (WHERE estado_final = 'CANCELADA') AS canceladas,
                       count(*) FILTER (WHERE estado_final = 'NO_SHOW')   AS no_shows,
                       COALESCE(sum(penalizacion), 0)                    AS penalizacion_total
                  FROM reportes.hechos_reserva
                 WHERE negocio_id = ? AND ocurrido_en >= ? AND ocurrido_en < ?
                """,
                (rs, fila) -> {
                    int total = rs.getInt("total");
                    int canceladas = rs.getInt("canceladas");
                    int noShows = rs.getInt("no_shows");
                    return new CancelacionesDelPeriodo(total, canceladas, noShows,
                            dividir(BigDecimal.valueOf(canceladas), BigDecimal.valueOf(total)),
                            dividir(BigDecimal.valueOf(noShows), BigDecimal.valueOf(total)),
                            rs.getBigDecimal("penalizacion_total"));
                },
                negocioId, rango[0], rango[1]);
    }

    private OffsetDateTime[] rango(UUID negocioId, LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new ReglaDeNegocioException("El rango de fechas no es válido");
        }
        ZoneId zona = ZoneId.of(zonas.de(negocioId));
        return new OffsetDateTime[] {desde.atStartOfDay(zona).toOffsetDateTime(),
                hasta.plusDays(1).atStartOfDay(zona).toOffsetDateTime()};
    }

    private static BigDecimal dividir(BigDecimal numerador, BigDecimal divisor) {
        if (numerador == null || divisor == null || divisor.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return numerador.divide(divisor, 4, java.math.RoundingMode.HALF_UP);
    }
}
