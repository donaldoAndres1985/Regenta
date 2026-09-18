package com.regenta.reportes.aplicacion;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;

/**
 * Programar un reporte para que llegue solo (HU-100 criterio 2).
 *
 * <p>El barrido corre dentro del negocio que lo dispara, igual que el resto de
 * los barridos del sistema: el {@code @Scheduled} que recorrería todos los
 * negocios de una vez necesita un rol privilegiado que todavía no existe.
 *
 * <p>Cada programación crea su fila en {@code definiciones_reporte}, que va
 * filtrada por negocio: la tabla admite {@code negocio_id} nulo para reportes
 * "de sistema", pero con RLS una fila sin negocio no la ve nadie, así que cada
 * negocio tiene la suya.
 */
@Service
public class GestionDeProgramaciones {

    private static final int DIAS_POR_DEFECTO = 30;

    private final JdbcTemplate jdbc;
    private final CatalogoDeReportes catalogo;
    private final ExportadorDeReportes exportador;
    private final GestionDeExportaciones exportaciones;
    private final ZonaHorariaDeNegocios zonas;

    public GestionDeProgramaciones(JdbcTemplate jdbc, CatalogoDeReportes catalogo,
            ExportadorDeReportes exportador, GestionDeExportaciones exportaciones,
            ZonaHorariaDeNegocios zonas) {
        this.jdbc = jdbc;
        this.catalogo = catalogo;
        this.exportador = exportador;
        this.exportaciones = exportaciones;
        this.zonas = zonas;
    }

    @Transactional
    @RequierePermiso("REPORTES_REPORTE_EXPORTAR")
    public ReporteProgramado programar(SolicitudDeProgramacion solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        ReporteExportable reporte = catalogo.exigir(solicitud.codigo());
        exportador.tipoMime(solicitud.formato());
        CronExpression cron = cronValido(solicitud.cron());
        int dias = solicitud.dias() == null || solicitud.dias() <= 0
                ? DIAS_POR_DEFECTO : solicitud.dias();

        UUID definicionId = definicionDe(negocioId, reporte);
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO reportes.reportes_programados (id, negocio_id, definicion_id, nombre, cron,
                    parametros, formato, destinatarios, activo, proxima_ejecucion)
                VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, ?, true, ?)
                """,
                id, negocioId, definicionId, solicitud.nombre().trim(), solicitud.cron().trim(),
                "{\"dias\":" + dias + "}", solicitud.formato().trim().toUpperCase(Locale.ROOT),
                destinatarios(solicitud.destinatarios()), proxima(cron, negocioId));
        return ver(id);
    }

    @Transactional(readOnly = true)
    @RequierePermiso("REPORTES_REPORTE_VER")
    public List<ReporteProgramado> listar() {
        return jdbc.query(SELECCION + " WHERE p.negocio_id = ? ORDER BY p.nombre",
                (rs, n) -> fila(rs), ContextoDeNegocio.negocioActual());
    }

    @Transactional(readOnly = true)
    @RequierePermiso("REPORTES_REPORTE_VER")
    public ReporteProgramado ver(UUID id) {
        return jdbc.query(SELECCION + " WHERE p.id = ? AND p.negocio_id = ?",
                rs -> {
                    if (!rs.next()) {
                        throw new NoEncontradoException("Esa programación no existe");
                    }
                    return fila(rs);
                },
                id, ContextoDeNegocio.negocioActual());
    }

    /**
     * Criterio 2: encola lo que ya tenía que haber corrido y vuelve a
     * agendarlo. Encolar y reagendar van en la misma transacción: si se
     * encolara sin reagendar, el siguiente barrido lo volvería a encolar.
     */
    @Transactional
    @RequierePermiso("REPORTES_REPORTE_EXPORTAR")
    public int barrer() {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        List<Map<String, Object>> vencidas = jdbc.queryForList("""
                SELECT id, cron, formato, parametros::text AS parametros,
                       (SELECT d.codigo FROM reportes.definiciones_reporte d
                         WHERE d.id = p.definicion_id) AS codigo
                  FROM reportes.reportes_programados p
                 WHERE p.negocio_id = ? AND p.activo
                   AND p.proxima_ejecucion IS NOT NULL AND p.proxima_ejecucion <= now()
                 FOR UPDATE SKIP LOCKED
                """, negocioId);

        for (Map<String, Object> programada : vencidas) {
            int dias = diasDe((String) programada.get("parametros"));
            LocalDate hasta = LocalDate.now(ZoneId.of(zonas.de(negocioId)));
            exportaciones.encolar((String) programada.get("codigo"),
                    (String) programada.get("formato"), hasta.minusDays(dias), hasta,
                    (UUID) programada.get("id"));
            jdbc.update("UPDATE reportes.reportes_programados SET proxima_ejecucion = ? WHERE id = ?",
                    proxima(cronValido((String) programada.get("cron")), negocioId),
                    programada.get("id"));
        }
        return vencidas.size();
    }

    /**
     * Una definición por negocio y código. El {@code codigo} es el del
     * catálogo y no cambia; el nombre sí puede, así que se refresca.
     */
    private UUID definicionDe(UUID negocioId, ReporteExportable reporte) {
        UUID existente = jdbc.query("""
                SELECT id FROM reportes.definiciones_reporte
                 WHERE negocio_id = ? AND codigo = ?
                """, rs -> rs.next() ? rs.getObject(1, UUID.class) : null, negocioId, reporte.codigo());
        if (existente != null) {
            return existente;
        }
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO reportes.definiciones_reporte (id, negocio_id, codigo, nombre, patron,
                    consulta, columnas, activo)
                VALUES (?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, true)
                """,
                id, negocioId, reporte.codigo(), reporte.nombre(), reporte.patron(),
                "{\"tipo\":\"CATALOGO\",\"codigo\":\"" + reporte.codigo() + "\"}",
                columnasJson(reporte.columnas()));
        return id;
    }

    private OffsetDateTime proxima(CronExpression cron, UUID negocioId) {
        ZoneId zona = ZoneId.of(zonas.de(negocioId));
        ZonedDateTime siguiente = cron.next(ZonedDateTime.now(zona));
        if (siguiente == null) {
            throw new ReglaDeNegocioException("Ese cron no vuelve a ocurrir nunca");
        }
        return siguiente.toOffsetDateTime();
    }

    private static CronExpression cronValido(String cron) {
        try {
            return CronExpression.parse(cron.trim());
        } catch (IllegalArgumentException noSeEntiende) {
            throw new ReglaDeNegocioException(
                    "No se entiende esa programación: " + cron + " (formato cron de 6 campos)");
        }
    }

    private static int diasDe(String parametros) {
        if (parametros == null) {
            return DIAS_POR_DEFECTO;
        }
        var encontrado = java.util.regex.Pattern.compile("\"dias\"\\s*:\\s*(\\d+)").matcher(parametros);
        return encontrado.find() ? Integer.parseInt(encontrado.group(1)) : DIAS_POR_DEFECTO;
    }

    private static String[] destinatarios(List<String> destinatarios) {
        return destinatarios == null ? new String[0]
                : destinatarios.stream().filter(d -> d != null && !d.isBlank())
                        .map(String::trim).toArray(String[]::new);
    }

    private static String columnasJson(List<String> columnas) {
        return columnas.stream().map(c -> "\"" + c.replace("\"", "\\\"") + "\"")
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }

    private static final String SELECCION = """
            SELECT p.id, p.nombre, p.cron, p.formato, p.destinatarios, p.activo, p.proxima_ejecucion,
                   p.parametros::text AS parametros,
                   (SELECT d.codigo FROM reportes.definiciones_reporte d WHERE d.id = p.definicion_id)
                       AS codigo
              FROM reportes.reportes_programados p
            """;

    private static ReporteProgramado fila(java.sql.ResultSet rs) throws java.sql.SQLException {
        java.sql.Array destinos = rs.getArray("destinatarios");
        return new ReporteProgramado(rs.getObject("id", UUID.class), rs.getString("codigo"),
                rs.getString("nombre"), rs.getString("cron"), rs.getString("formato"),
                destinos == null ? List.of() : List.of((String[]) destinos.getArray()),
                diasDe(rs.getString("parametros")), rs.getBoolean("activo"),
                rs.getObject("proxima_ejecucion", OffsetDateTime.class));
    }
}
