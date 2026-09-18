package com.regenta.reportes.aplicacion;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;

/**
 * Exportar un reporte (HU-100 criterios 1, 3 y 4).
 *
 * <p>Pedir una exportación no la genera: crea la fila en {@code
 * ejecuciones_reporte} y devuelve el comprobante. Un reporte de un año no
 * puede dejar colgada la pantalla de quien lo pidió ni ocupar un hilo de la
 * API mientras arma un XLSX de cien mil filas (criterio 4).
 *
 * <p>El barrido se dispara dentro del negocio que lo llama, igual que el resto
 * de los barridos del sistema (HU-094): el {@code @Scheduled} que recorrería
 * todos los negocios de una vez necesita un rol privilegiado que todavía no
 * existe y queda diferido.
 */
@Service
public class GestionDeExportaciones {

    private final JdbcTemplate jdbc;
    private final CatalogoDeReportes catalogo;
    private final ExportadorDeReportes exportador;
    private final GeneradorDeExportaciones generador;

    public GestionDeExportaciones(JdbcTemplate jdbc, CatalogoDeReportes catalogo,
            ExportadorDeReportes exportador, GeneradorDeExportaciones generador) {
        this.jdbc = jdbc;
        this.catalogo = catalogo;
        this.exportador = exportador;
        this.generador = generador;
    }

    /** Criterio 4: responde con el comprobante, no con el archivo. */
    @Transactional
    @RequierePermiso("REPORTES_REPORTE_EXPORTAR")
    public EjecucionDeReporte solicitar(SolicitudDeExportacion solicitud) {
        UUID id = encolar(solicitud.codigo(), solicitud.formato(), solicitud.desde(),
                solicitud.hasta(), null);
        return ver(id);
    }

    /**
     * Deja la ejecución lista para que la tome el barrido. Lo usa también la
     * programación, que no pasa por el permiso de exportar: quien programó ya
     * lo tenía.
     */
    @Transactional
    public UUID encolar(String codigo, String formato, LocalDate desde, LocalDate hasta,
            UUID programadoId) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        ReporteExportable reporte = catalogo.exigir(codigo);
        // Que el formato sea uno de los tres se comprueba ahora y no en el
        // barrido: un formato mal escrito es un error de quien pide, y tiene
        // que enterarse en la respuesta, no en un historial media hora después.
        exportador.tipoMime(formato);

        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO reportes.ejecuciones_reporte (id, negocio_id, codigo, formato, parametros,
                    programado_id, estado, intentos, proximo_intento_en, usuario_id)
                VALUES (?, ?, ?, ?, ?::jsonb, ?, 'EN_CURSO', 0, now(), ?)
                """,
                id, negocioId, reporte.codigo(), formato.trim().toUpperCase(Locale.ROOT),
                parametros(desde, hasta), programadoId, ContextoDeNegocio.actual().usuario());
        return id;
    }

    /**
     * Genera lo que esté pendiente de este negocio: lo recién pedido y lo que
     * falló y ya cumplió su espera (criterio 3).
     */
    @RequierePermiso("REPORTES_REPORTE_EXPORTAR")
    public int procesarPendientes() {
        List<Map<String, Object>> pendientes = generador.pendientes();
        for (Map<String, Object> fila : pendientes) {
            generador.generar(fila);
        }
        return pendientes.size();
    }

    @Transactional(readOnly = true)
    @RequierePermiso("REPORTES_REPORTE_VER")
    public EjecucionDeReporte ver(UUID ejecucionId) {
        return jdbc.query(SELECCION + " WHERE e.id = ? AND e.negocio_id = ?",
                rs -> {
                    if (!rs.next()) {
                        throw new NoEncontradoException("Esa exportación no existe");
                    }
                    return fila(rs);
                },
                ejecucionId, ContextoDeNegocio.negocioActual());
    }

    @Transactional(readOnly = true)
    @RequierePermiso("REPORTES_REPORTE_VER")
    public List<EjecucionDeReporte> historial() {
        return jdbc.query(SELECCION + " WHERE e.negocio_id = ? ORDER BY e.iniciado_en DESC LIMIT 100",
                (rs, n) -> fila(rs), ContextoDeNegocio.negocioActual());
    }

    @Transactional(readOnly = true)
    @RequierePermiso("REPORTES_REPORTE_VER")
    public ArchivoDeReporte archivo(UUID ejecucionId) {
        return jdbc.query("""
                SELECT nombre, tipo_mime, contenido FROM reportes.archivos_reporte
                 WHERE ejecucion_id = ? AND negocio_id = ?
                """,
                rs -> {
                    if (!rs.next()) {
                        throw new NoEncontradoException("Esa exportación todavía no tiene archivo");
                    }
                    return new ArchivoDeReporte(rs.getString(1), rs.getString(2), rs.getBytes(3));
                },
                ejecucionId, ContextoDeNegocio.negocioActual());
    }

    private static String parametros(LocalDate desde, LocalDate hasta) {
        return "{\"desde\":" + comillas(desde) + ",\"hasta\":" + comillas(hasta) + "}";
    }

    private static String comillas(LocalDate fecha) {
        return fecha == null ? "null" : "\"" + fecha + "\"";
    }

    private static final String SELECCION = """
            SELECT e.id, e.codigo, e.formato, e.estado, e.filas, e.error, e.intentos,
                   e.iniciado_en, e.finalizado_en,
                   (SELECT a.nombre FROM reportes.archivos_reporte a WHERE a.ejecucion_id = e.id)
                       AS archivo_nombre
              FROM reportes.ejecuciones_reporte e
            """;

    private static EjecucionDeReporte fila(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new EjecucionDeReporte(rs.getObject("id", UUID.class), rs.getString("codigo"),
                rs.getString("formato"), rs.getString("estado"), (Integer) rs.getObject("filas"),
                rs.getString("archivo_nombre"), rs.getString("error"), rs.getInt("intentos"),
                rs.getObject("iniciado_en", OffsetDateTime.class),
                rs.getObject("finalizado_en", OffsetDateTime.class));
    }
}
