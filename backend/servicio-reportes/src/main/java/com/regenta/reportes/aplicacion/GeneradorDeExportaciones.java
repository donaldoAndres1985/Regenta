package com.regenta.reportes.aplicacion;

import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.eventos.RegistroDeEventos;

/**
 * Genera de verdad el archivo de una ejecución pendiente (HU-100 criterios 3
 * y 4). Es un bean aparte de {@link GestionDeExportaciones} por una razón
 * concreta: {@code REQUIRES_NEW} no aplica si el método se llama desde la
 * misma clase —Spring no pasa por el proxy— y aquí hace falta de verdad, que
 * una exportación falle no puede tirar abajo las que ya se generaron en la
 * misma pasada.
 */
@Component
public class GeneradorDeExportaciones {

    private static final Logger log = LoggerFactory.getLogger(GeneradorDeExportaciones.class);

    /** Después de tres intentos lo que falla no es un tropiezo, es un bug. */
    public static final int INTENTOS_MAXIMOS = 3;

    /** Cuántas toma cada barrido: suficiente para no dejar cola, poco para no ahogar la base. */
    private static final int POR_BARRIDO = 20;

    /** Cuánto se da por buena una ejecución tomada antes de considerarla abandonada. */
    private static final Duration ARRIENDO = Duration.ofMinutes(10);

    private final JdbcTemplate jdbc;
    private final CatalogoDeReportes catalogo;
    private final ExportadorDeReportes exportador;
    private final RegistroDeEventos eventos;
    private final PasarelaDeCorreo correo;

    public GeneradorDeExportaciones(JdbcTemplate jdbc, CatalogoDeReportes catalogo,
            ExportadorDeReportes exportador, RegistroDeEventos eventos, PasarelaDeCorreo correo) {
        this.jdbc = jdbc;
        this.catalogo = catalogo;
        this.exportador = exportador;
        this.eventos = eventos;
        this.correo = correo;
    }

    /**
     * Toma las pendientes y las deja arrendadas por {@link #ARRIENDO}.
     *
     * <p>{@code SKIP LOCKED} sirve mientras dura esta transacción, y esta
     * transacción termina antes de generar nada —cada archivo se genera en la
     * suya, para que un fallo no arrastre a los demás—. Así que además de
     * tomarlas se les corre la fecha de reintento hacia adelante: otra
     * instancia que barra al mismo tiempo no las vuelve a ver, y si este
     * proceso se muere a mitad de camino, vuelven solas cuando vence el
     * arriendo en vez de quedarse colgadas para siempre.
     */
    @Transactional
    public List<Map<String, Object>> pendientes() {
        List<Map<String, Object>> tomadas = jdbc.queryForList("""
                SELECT id, negocio_id, codigo, formato, parametros::text AS parametros, intentos,
                       programado_id
                  FROM reportes.ejecuciones_reporte
                 WHERE estado IN ('EN_CURSO', 'FALLIDO')
                   AND proximo_intento_en IS NOT NULL
                   AND proximo_intento_en <= now()
                 ORDER BY proximo_intento_en
                 LIMIT ?
                 FOR UPDATE SKIP LOCKED
                """, POR_BARRIDO);
        for (Map<String, Object> fila : tomadas) {
            jdbc.update("UPDATE reportes.ejecuciones_reporte SET proximo_intento_en = now() + ?::interval"
                    + " WHERE id = ?", ARRIENDO.toMinutes() + " minutes", fila.get("id"));
        }
        return tomadas;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generar(Map<String, Object> fila) {
        UUID id = (UUID) fila.get("id");
        UUID negocioId = (UUID) fila.get("negocio_id");
        int intentos = ((Number) fila.get("intentos")).intValue() + 1;
        long empezo = System.currentTimeMillis();
        try {
            ReporteExportable reporte = catalogo.exigir((String) fila.get("codigo"));
            String formato = (String) fila.get("formato");
            LocalDate desde = fechaDe((String) fila.get("parametros"), "desde");
            LocalDate hasta = fechaDe((String) fila.get("parametros"), "hasta");

            List<List<Object>> filas = reporte.filas(desde, hasta);
            byte[] contenido = exportador.generar(formato, reporte.nombre(), reporte.columnas(), filas);
            String nombre = exportador.nombreDeArchivo(reporte.nombre(), formato);

            jdbc.update("""
                    INSERT INTO reportes.archivos_reporte (ejecucion_id, negocio_id, nombre, tipo_mime,
                        bytes, contenido)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON CONFLICT (ejecucion_id) DO UPDATE SET nombre = EXCLUDED.nombre,
                        tipo_mime = EXCLUDED.tipo_mime, bytes = EXCLUDED.bytes,
                        contenido = EXCLUDED.contenido, creado_en = now()
                    """,
                    id, negocioId, nombre, exportador.tipoMime(formato), contenido.length, contenido);

            jdbc.update("""
                    UPDATE reportes.ejecuciones_reporte
                       SET estado = 'COMPLETADO', filas = ?, error = NULL, intentos = ?,
                           proximo_intento_en = NULL, duracion_ms = ?, finalizado_en = now()
                     WHERE id = ?
                    """, filas.size(), intentos, (int) (System.currentTimeMillis() - empezo), id);

            avisar(negocioId, id, nombre, filas.size());
            enviarSiEstabaProgramado(fila, reporte, new ArchivoDeReporte(nombre,
                    exportador.tipoMime(formato), contenido));
        } catch (RuntimeException fallo) {
            anotarElFallo(id, intentos, fallo);
        }
    }

    /**
     * Criterio 3. El error queda escrito tal como vino: quien lo lea mañana no
     * tiene a mano los logs de hoy. Y al tercer intento deja de reintentarse —
     * reintentar para siempre no es una garantía, es un bucle.
     */
    private void anotarElFallo(UUID id, int intentos, RuntimeException fallo) {
        log.warn("Falló la exportación {} (intento {}): {}", id, intentos, fallo.getMessage());
        boolean seSigueIntentando = intentos < INTENTOS_MAXIMOS;
        jdbc.update("""
                UPDATE reportes.ejecuciones_reporte
                   SET estado = 'FALLIDO', error = ?, intentos = ?, finalizado_en = now(),
                       proximo_intento_en = CASE WHEN ?::boolean
                                            THEN now() + (?::text || ' seconds')::interval END
                 WHERE id = ?
                """,
                mensajeDe(fallo), intentos, seSigueIntentando,
                String.valueOf(esperaTrasFallo(intentos).toSeconds()), id);
    }

    /** 1, 4 y 9 minutos: le da tiempo a recuperarse a lo que se haya caído. */
    static Duration esperaTrasFallo(int intentos) {
        return Duration.ofMinutes((long) intentos * intentos);
    }

    /**
     * Criterio 2: el que se programó se manda a sus destinatarios. Si el
     * correo falla, el reporte igual quedó generado y descargable: perder el
     * archivo porque el servidor de correo está caído sería peor que no
     * mandarlo. Queda en el log y se ve en el historial.
     */
    private void enviarSiEstabaProgramado(Map<String, Object> fila, ReporteExportable reporte,
            ArchivoDeReporte archivo) {
        UUID programadoId = (UUID) fila.get("programado_id");
        if (programadoId == null) {
            return;
        }
        List<String> destinatarios = destinatariosDe(programadoId);
        String asunto = reporte.nombre() + " · " + nombreDelProgramado(programadoId);
        for (String destino : destinatarios) {
            var resultado = correo.enviar(destino, asunto,
                    "Adjunto va el reporte programado. Lo genera Regenta automáticamente.", archivo);
            if (!resultado.ok()) {
                log.warn("No se pudo enviar el reporte {} a {}: {}", fila.get("id"), destino,
                        resultado.error());
            }
        }
    }

    private List<String> destinatariosDe(UUID programadoId) {
        return jdbc.query("SELECT destinatarios FROM reportes.reportes_programados WHERE id = ?",
                rs -> {
                    if (!rs.next() || rs.getArray(1) == null) {
                        return List.<String>of();
                    }
                    return List.of((String[]) rs.getArray(1).getArray());
                }, programadoId);
    }

    private String nombreDelProgramado(UUID programadoId) {
        String nombre = jdbc.query("SELECT nombre FROM reportes.reportes_programados WHERE id = ?",
                rs -> rs.next() ? rs.getString(1) : null, programadoId);
        return nombre == null ? "programado" : nombre;
    }

    /** Criterio 4: "se avisa cuando está listo". */
    private void avisar(UUID negocioId, UUID ejecucionId, String nombre, int filas) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("negocio_id", negocioId.toString());
        datos.put("ejecucion_id", ejecucionId.toString());
        datos.put("archivo", nombre);
        datos.put("filas", filas);
        eventos.registrar(negocioId, "EjecucionDeReporte", ejecucionId, "reporte_listo", datos);
    }

    static LocalDate fechaDe(String parametros, String clave) {
        if (parametros == null) {
            return null;
        }
        var encontrada = java.util.regex.Pattern
                .compile("\"" + clave + "\"\\s*:\\s*\"([0-9-]{10})\"").matcher(parametros);
        return encontrada.find() ? LocalDate.parse(encontrada.group(1)) : null;
    }

    private static String mensajeDe(RuntimeException fallo) {
        String mensaje = fallo.getMessage();
        return mensaje == null || mensaje.isBlank() ? fallo.getClass().getSimpleName() : mensaje;
    }
}
