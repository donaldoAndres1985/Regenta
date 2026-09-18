package com.regenta.reportes.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;

/**
 * Las métricas del patrón Comanda (HU-099 criterios 2, 3 y 4): rotación de
 * mesas, tiempo medio de mesa, ticket por comensal y demora de cocina por
 * plato.
 *
 * <p>{@code hechos_comanda} tiene grano de línea, así que todo lo que es "por
 * comanda" —el tiempo de mesa, los comensales— se agrupa primero por comanda
 * antes de promediar. Promediar directo sobre las líneas le daría el triple de
 * peso a la mesa que pidió tres platos.
 */
@Service
public class MetricasDeComanda {

    private final JdbcTemplate jdbc;
    private final ZonaHorariaDeNegocios zonas;

    public MetricasDeComanda(JdbcTemplate jdbc, ZonaHorariaDeNegocios zonas) {
        this.jdbc = jdbc;
        this.zonas = zonas;
    }

    /** Criterio 2. */
    @Transactional(readOnly = true)
    @RequierePermiso("REPORTES_REPORTE_VER")
    public MesasDelPeriodo mesas(LocalDate desde, LocalDate hasta) {
        PatronDelNegocio.exigir("COMANDA", "rotación de mesas");
        UUID negocioId = ContextoDeNegocio.negocioActual();
        OffsetDateTime[] rango = rango(negocioId, desde, hasta);

        return jdbc.queryForObject("""
                WITH por_comanda AS (
                    -- mesa_id y num_comensales son de la comanda, no de la
                    -- linea: van en el GROUP BY porque se repiten identicos en
                    -- cada linea. PostgreSQL no tiene max(uuid).
                    SELECT comanda_id, mesa_id,
                           max(num_comensales)     AS comensales,
                           max(tiempo_mesa_min)    AS minutos,
                           sum(monto_neto)         AS neto
                      FROM reportes.hechos_comanda
                     WHERE negocio_id = ? AND ocurrido_en >= ? AND ocurrido_en < ?
                     GROUP BY comanda_id, mesa_id
                )
                SELECT count(*)                                        AS comandas,
                       count(DISTINCT mesa_id)                         AS mesas_usadas,
                       COALESCE(sum(comensales), 0)                    AS comensales,
                       COALESCE(sum(neto), 0)                          AS neto,
                       COALESCE(avg(minutos), 0)                       AS minutos_medios
                  FROM por_comanda
                """,
                (rs, fila) -> {
                    int comandas = rs.getInt("comandas");
                    int mesasUsadas = rs.getInt("mesas_usadas");
                    int comensales = rs.getInt("comensales");
                    BigDecimal neto = rs.getBigDecimal("neto");
                    return new MesasDelPeriodo(comandas, mesasUsadas, comensales,
                            dividir(BigDecimal.valueOf(comandas), BigDecimal.valueOf(mesasUsadas)),
                            rs.getBigDecimal("minutos_medios").setScale(4, java.math.RoundingMode.HALF_UP),
                            neto,
                            dividir(neto, BigDecimal.valueOf(comensales)),
                            dividir(neto, BigDecimal.valueOf(mesasUsadas)));
                },
                negocioId, rango[0], rango[1]);
    }

    /**
     * Criterio 4. Las líneas sin marca de cocina no entran: un plato que nunca
     * pasó por el KDS no se preparó en cero minutos, simplemente no se sabe
     * cuánto tardó, y contarlo como cero bajaría el promedio con un dato falso.
     */
    @Transactional(readOnly = true)
    @RequierePermiso("REPORTES_REPORTE_VER")
    public List<PreparacionDePlato> preparacion(LocalDate desde, LocalDate hasta) {
        PatronDelNegocio.exigir("COMANDA", "tiempos de cocina");
        UUID negocioId = ContextoDeNegocio.negocioActual();
        OffsetDateTime[] rango = rango(negocioId, desde, hasta);

        return jdbc.query("""
                SELECT item_nombre,
                       count(*)                    AS veces,
                       avg(tiempo_preparacion_min) AS promedio,
                       max(tiempo_preparacion_min) AS maximo
                  FROM reportes.hechos_comanda
                 WHERE negocio_id = ? AND ocurrido_en >= ? AND ocurrido_en < ?
                   AND tiempo_preparacion_min IS NOT NULL
                 GROUP BY item_nombre
                 ORDER BY promedio DESC, item_nombre
                """,
                (rs, fila) -> new PreparacionDePlato(rs.getString("item_nombre"), rs.getInt("veces"),
                        rs.getBigDecimal("promedio").setScale(4, java.math.RoundingMode.HALF_UP),
                        (Integer) rs.getObject("maximo")),
                negocioId, rango[0], rango[1]);
    }

    /** El rango en la zona horaria del negocio: {@code hasta} incluido. */
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
