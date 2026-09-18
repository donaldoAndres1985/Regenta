package com.regenta.reportes.aplicacion;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;

/**
 * HU-098: qué se vende, con qué margen, y qué está quieto. A diferencia de
 * {@link PanelDiario}, sí recorre las tablas de hechos —es el reporte
 * detallado, no el panel que tiene que responder al instante— pero siempre
 * acotado a un rango de fechas y al negocio actual.
 */
@Service
public class ReporteDeVentas {

    private final JdbcTemplate jdbc;
    private final ZonaHorariaDeNegocios zonas;

    public ReporteDeVentas(JdbcTemplate jdbc, ZonaHorariaDeNegocios zonas) {
        this.jdbc = jdbc;
        this.zonas = zonas;
    }

    /** Criterios 1, 2 y 4: monto, unidades y margen por categoría, con el costo de cada venta y por sucursal. */
    @Transactional(readOnly = true)
    @RequierePermiso("REPORTES_REPORTE_VER")
    public List<VentaPorCategoria> porCategoria(LocalDate desde, LocalDate hasta, UUID sucursalId) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new ReglaDeNegocioException("El rango de fechas no es válido");
        }
        UUID negocioId = ContextoDeNegocio.negocioActual();
        ZoneId zona = ZoneId.of(zonas.de(negocioId));
        OffsetDateTime inicio = desde.atStartOfDay(zona).toOffsetDateTime();
        OffsetDateTime fin = hasta.plusDays(1).atStartOfDay(zona).toOffsetDateTime();

        StringBuilder sql = new StringBuilder("""
                SELECT COALESCE(dp.categoria_nombre, 'Sin categoría') AS categoria,
                       COALESCE(SUM(hv.monto_neto), 0) AS monto,
                       COALESCE(SUM(hv.cantidad), 0) AS unidades,
                       COALESCE(SUM(hv.margen), 0) AS margen
                FROM reportes.hechos_venta hv
                JOIN reportes.dim_producto dp ON dp.sk = hv.producto_sk
                WHERE hv.negocio_id = ? AND hv.ocurrido_en >= ? AND hv.ocurrido_en < ?
                """);
        List<Object> parametros = new ArrayList<>(List.of(negocioId, inicio, fin));
        if (sucursalId != null) {
            sql.append(" AND hv.sucursal_sk = (SELECT sk FROM reportes.dim_sucursal "
                    + "WHERE negocio_id = ? AND sucursal_id = ? AND es_actual)");
            parametros.add(negocioId);
            parametros.add(sucursalId);
        }
        sql.append(" GROUP BY COALESCE(dp.categoria_nombre, 'Sin categoría') ORDER BY monto DESC");

        return jdbc.query(sql.toString(),
                (rs, fila) -> new VentaPorCategoria(rs.getString("categoria"), rs.getBigDecimal("monto"),
                        rs.getBigDecimal("unidades"), rs.getBigDecimal("margen")),
                parametros.toArray());
    }

    /** Criterio 3: días sin movimiento de cada producto vigente del negocio. */
    @Transactional(readOnly = true)
    @RequierePermiso("REPORTES_REPORTE_VER")
    public List<RotacionProducto> rotacion(UUID sucursalId) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        ZoneId zona = ZoneId.of(zonas.de(negocioId));
        LocalDate hoy = LocalDate.now(zona);

        StringBuilder sql = new StringBuilder("""
                SELECT dp.producto_id AS producto_id, dp.nombre AS nombre,
                       MAX(hi.ocurrido_en) AS ultimo_movimiento
                FROM reportes.dim_producto dp
                LEFT JOIN reportes.hechos_inventario hi
                    ON hi.producto_sk = dp.sk AND hi.negocio_id = dp.negocio_id
                """);
        List<Object> parametros = new ArrayList<>();
        if (sucursalId != null) {
            sql.append(" AND hi.bodega_id = ?");
            parametros.add(sucursalId);
        }
        sql.append(" WHERE dp.negocio_id = ? AND dp.es_actual"
                + " GROUP BY dp.producto_id, dp.nombre ORDER BY dp.nombre");
        parametros.add(negocioId);

        return jdbc.query(sql.toString(), (rs, fila) -> {
            UUID productoId = UUID.fromString(rs.getString("producto_id"));
            String nombre = rs.getString("nombre");
            OffsetDateTime ultimo = rs.getObject("ultimo_movimiento", OffsetDateTime.class);
            LocalDate fecha = ultimo == null ? null : ultimo.atZoneSameInstant(zona).toLocalDate();
            Long dias = ultimo == null ? null : ChronoUnit.DAYS.between(fecha, hoy);
            return new RotacionProducto(productoId, nombre, fecha, dias);
        }, parametros.toArray());
    }
}
