package com.regenta.reportes.application;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.shared.almacen.AlmacenDeObjetos;
import com.regenta.shared.almacen.AlmacenNoDisponibleException;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.web.NoEncontradoException;
import com.regenta.shared.web.RecursoExpiradoException;
import com.regenta.shared.web.ServicioNoDisponibleException;

/**
 * HU-121 · Los archivos exportados viven en el almacén de objetos; en {@code reportes.archivos_reporte}
 * solo su URL (C1). Se descargan siempre por el backend, con la sesión del negocio dueño (C2). Los que
 * quedaron en la base antes de esto se suben con un barrido (C4), y al pasar su retención se borran
 * solos y quedan como expirados, no como fallidos (C5).
 */
@Component
public class ArchivosDeReporte {

    private static final Logger LOG = LoggerFactory.getLogger(ArchivosDeReporte.class);
    static final String CARPETA = "reportes";

    private final JdbcTemplate jdbc;
    private final AlmacenDeObjetos almacen;
    private final Duration retencion;

    public ArchivosDeReporte(JdbcTemplate jdbc, AlmacenDeObjetos almacen,
            @Value("${regenta.reportes.retencion-de-archivos:30d}") Duration retencion) {
        this.jdbc = jdbc;
        this.almacen = almacen;
        this.retencion = retencion;
    }

    /**
     * C1 · Sube el archivo y deja su URL. Si el almacén no responde lanza
     * {@link AlmacenNoDisponibleException}: la exportación queda FALLIDA y se reintenta como cualquier
     * otro fallo (HU-100). Si la ejecución ya tenía archivo, el viejo se borra del almacén.
     */
    public void guardar(UUID negocioId, UUID ejecucionId, String nombre, String tipoMime, byte[] contenido) {
        String anterior = urlDe(ejecucionId);
        String url = almacen.guardar(negocioId, CARPETA, nombre, contenido, tipoMime);
        jdbc.update("""
                INSERT INTO reportes.archivos_reporte (ejecucion_id, negocio_id, nombre, tipo_mime, bytes,
                    contenido, url, expira_en, expirado_en)
                VALUES (?, ?, ?, ?, ?, NULL, ?, ?, NULL)
                ON CONFLICT (ejecucion_id) DO UPDATE SET nombre = EXCLUDED.nombre,
                    tipo_mime = EXCLUDED.tipo_mime, bytes = EXCLUDED.bytes, contenido = NULL,
                    url = EXCLUDED.url, expira_en = EXCLUDED.expira_en, expirado_en = NULL, creado_en = now()
                """, ejecucionId, negocioId, nombre, tipoMime, contenido.length, url,
                Timestamp.from(Instant.now().plus(retencion)));
        if (anterior != null) {
            borrarSinFallar(negocioId, anterior);
        }
    }

    /** C2 · El archivo de una ejecución del negocio actual. 410 si expiró; 503 si el almacén no responde. */
    public ArchivoDeReporte leer(UUID ejecucionId) {
        UUID negocio = ContextoDeNegocio.negocioActual();
        List<Map<String, Object>> filas = jdbc.queryForList("""
                SELECT nombre, tipo_mime, contenido, url, expirado_en FROM reportes.archivos_reporte
                 WHERE ejecucion_id = ? AND negocio_id = ?
                """, ejecucionId, negocio);
        if (filas.isEmpty()) {
            throw new NoEncontradoException("Esa exportación todavía no tiene archivo");
        }
        Map<String, Object> f = filas.get(0);
        String nombre = (String) f.get("nombre");
        String tipo = (String) f.get("tipo_mime");
        if (f.get("expirado_en") != null) {
            throw new RecursoExpiradoException("El archivo de esta exportación ya expiró: pídela de nuevo");
        }
        if (f.get("contenido") != null) {
            return new ArchivoDeReporte(nombre, tipo, (byte[]) f.get("contenido")); // aún no subido (C4)
        }
        try {
            return new ArchivoDeReporte(nombre, tipo, almacen.leer(negocio, (String) f.get("url")));
        } catch (AlmacenNoDisponibleException caido) {
            throw new ServicioNoDisponibleException("El almacén de archivos no responde: intenta en un momento");
        }
    }

    /** C4 · Sube al almacén los archivos del negocio actual que todavía están en la base. */
    @Transactional
    public int subirLosDeLaBase() {
        UUID negocio = ContextoDeNegocio.negocioActual();
        List<Map<String, Object>> enLaBase = jdbc.queryForList("""
                SELECT ejecucion_id, nombre, tipo_mime, contenido, creado_en FROM reportes.archivos_reporte
                 WHERE negocio_id = ? AND contenido IS NOT NULL
                 LIMIT 50 FOR UPDATE SKIP LOCKED
                """, negocio);
        int subidos = 0;
        for (Map<String, Object> f : enLaBase) {
            String url = almacen.guardar(negocio, CARPETA, (String) f.get("nombre"), (byte[]) f.get("contenido"),
                    (String) f.get("tipo_mime"));
            Instant creado = ((Timestamp) f.get("creado_en")).toInstant();
            jdbc.update("""
                    UPDATE reportes.archivos_reporte
                       SET url = ?, contenido = NULL, expira_en = ?
                     WHERE ejecucion_id = ?
                    """, url, Timestamp.from(creado.plus(retencion)), f.get("ejecucion_id"));
            subidos++;
        }
        return subidos;
    }

    /** C5 · Borra del almacén los archivos vencidos del negocio actual y los marca expirados. */
    @Transactional
    public int borrarVencidos() {
        UUID negocio = ContextoDeNegocio.negocioActual();
        List<Map<String, Object>> vencidos = jdbc.queryForList("""
                SELECT ejecucion_id, url FROM reportes.archivos_reporte
                 WHERE negocio_id = ? AND url IS NOT NULL AND expirado_en IS NULL AND expira_en <= now()
                 LIMIT 200 FOR UPDATE SKIP LOCKED
                """, negocio);
        for (Map<String, Object> v : vencidos) {
            almacen.borrar(negocio, (String) v.get("url"));
            jdbc.update("UPDATE reportes.archivos_reporte SET url = NULL, expirado_en = now() WHERE ejecucion_id = ?",
                    v.get("ejecucion_id"));
        }
        return vencidos.size();
    }

    private String urlDe(UUID ejecucionId) {
        List<String> urls = jdbc.query("SELECT url FROM reportes.archivos_reporte WHERE ejecucion_id = ?",
                (rs, n) -> rs.getString(1), ejecucionId);
        return urls.isEmpty() ? null : urls.get(0);
    }

    private void borrarSinFallar(UUID negocioId, String url) {
        try {
            almacen.borrar(negocioId, url);
        } catch (RuntimeException fallo) {
            LOG.warn("No se pudo borrar el archivo anterior {}: {}", url, fallo.getMessage());
        }
    }
}
