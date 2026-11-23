package com.regenta.auditoria.application;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.zip.GZIPInputStream;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.shared.almacen.AlmacenDeObjetos;
import com.regenta.shared.almacen.AlmacenNoDisponibleException;
import com.regenta.shared.security.RequierePermiso;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.web.ServicioNoDisponibleException;

/**
 * Consultas de la bitácora de auditoría. HU-101 criterio 4, HU-105.
 *
 * <p>HU-131 C3: con {@code incluirArchivados}, también busca en los archivos de los eventos que ya
 * salieron de la tabla viva por retención. Tarda más (lee el almacén), por eso se pide aparte; los
 * archivados vienen marcados.
 */
@Service
public class ConsultaDeAuditoria {

    private static final Comparator<EventoDeAuditoria> MAS_RECIENTE_PRIMERO =
            Comparator.comparing(EventoDeAuditoria::ocurridoEn).reversed();

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final AlmacenDeObjetos almacen;

    public ConsultaDeAuditoria(JdbcTemplate jdbc, ObjectMapper json, AlmacenDeObjetos almacen) {
        this.jdbc = jdbc;
        this.json = json;
        this.almacen = almacen;
    }

    /** HU-101 criterio 4: reconstruye todo lo que hizo un request, aunque cruzara varios servicios. */
    @Transactional(readOnly = true)
    @RequierePermiso("AUDITORIA_BITACORA_VER")
    public List<EventoDeAuditoria> porTraceId(String traceId) {
        return porTraceId(traceId, false);
    }

    @Transactional(readOnly = true)
    @RequierePermiso("AUDITORIA_BITACORA_VER")
    public List<EventoDeAuditoria> porTraceId(String traceId, boolean incluirArchivados) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        List<EventoDeAuditoria> vivos = jdbc.query("""
                SELECT id, usuario_id, servicio, entidad_tipo, entidad_id, accion, trace_id, resultado,
                    ocurrido_en, cambios::text AS cambios
                FROM auditoria.eventos_auditoria
                WHERE negocio_id = ? AND trace_id = ?
                ORDER BY ocurrido_en
                """, this::fila, negocioId, traceId);
        if (!incluirArchivados) {
            return vivos;
        }
        List<EventoDeAuditoria> todos = juntar(vivos,
                archivados(negocioId, null, null, e -> traceId.equals(e.get("trace_id"))));
        todos.sort(Comparator.comparing(EventoDeAuditoria::ocurridoEn));
        return todos;
    }

    /** HU-105 criterio 1: el historial de un documento (una venta, un producto...), con autor y fecha. */
    @Transactional(readOnly = true)
    @RequierePermiso("AUDITORIA_BITACORA_VER")
    public List<EventoDeAuditoria> porEntidad(String entidadTipo, UUID entidadId) {
        return porEntidad(entidadTipo, entidadId, false);
    }

    @Transactional(readOnly = true)
    @RequierePermiso("AUDITORIA_BITACORA_VER")
    public List<EventoDeAuditoria> porEntidad(String entidadTipo, UUID entidadId, boolean incluirArchivados) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        List<EventoDeAuditoria> vivos = jdbc.query("""
                SELECT id, usuario_id, servicio, entidad_tipo, entidad_id, accion, trace_id, resultado,
                    ocurrido_en, cambios::text AS cambios
                FROM auditoria.eventos_auditoria
                WHERE negocio_id = ? AND entidad_tipo = ? AND entidad_id = ?
                ORDER BY ocurrido_en DESC
                """, this::fila, negocioId, entidadTipo, entidadId);
        if (!incluirArchivados) {
            return vivos;
        }
        List<EventoDeAuditoria> todos = juntar(vivos, archivados(negocioId, null, null,
                e -> entidadTipo.equals(e.get("entidad_tipo")) && entidadId.toString().equals(e.get("entidad_id"))));
        todos.sort(MAS_RECIENTE_PRIMERO);
        return todos;
    }

    /** HU-105 criterio 2: toda la actividad de un usuario en un rango (un día, típicamente). */
    @Transactional(readOnly = true)
    @RequierePermiso("AUDITORIA_BITACORA_VER")
    public List<EventoDeAuditoria> porUsuario(UUID usuarioId, OffsetDateTime desde, OffsetDateTime hasta) {
        return porUsuario(usuarioId, desde, hasta, false);
    }

    @Transactional(readOnly = true)
    @RequierePermiso("AUDITORIA_BITACORA_VER")
    public List<EventoDeAuditoria> porUsuario(UUID usuarioId, OffsetDateTime desde, OffsetDateTime hasta,
            boolean incluirArchivados) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        List<EventoDeAuditoria> vivos = jdbc.query("""
                SELECT id, usuario_id, servicio, entidad_tipo, entidad_id, accion, trace_id, resultado,
                    ocurrido_en, cambios::text AS cambios
                FROM auditoria.eventos_auditoria
                WHERE negocio_id = ? AND usuario_id = ? AND ocurrido_en >= ? AND ocurrido_en < ?
                ORDER BY ocurrido_en DESC
                """, this::fila, negocioId, usuarioId, desde, hasta);
        if (!incluirArchivados) {
            return vivos;
        }
        List<EventoDeAuditoria> todos = juntar(vivos, archivados(negocioId, desde, hasta, e -> {
            OffsetDateTime cuando = OffsetDateTime.parse((String) e.get("ocurrido_en"));
            return usuarioId.toString().equals(e.get("usuario_id")) && !cuando.isBefore(desde)
                    && cuando.isBefore(hasta);
        }));
        todos.sort(MAS_RECIENTE_PRIMERO);
        return todos;
    }

    /**
     * C3 · Los eventos archivados del negocio que cumplen el filtro. Con rango, solo se leen los archivos
     * que se cruzan con él; sin rango, todos los del negocio (por eso tarda más).
     */
    private List<EventoDeAuditoria> archivados(UUID negocioId, OffsetDateTime desde, OffsetDateTime hasta,
            Predicate<Map<String, Object>> filtro) {
        List<String> urls = desde == null
                ? jdbc.queryForList("SELECT url FROM auditoria.archivos_auditoria WHERE negocio_id = ? ORDER BY desde",
                        String.class, negocioId)
                : jdbc.queryForList("""
                        SELECT url FROM auditoria.archivos_auditoria
                         WHERE negocio_id = ? AND desde < ? AND hasta >= ? ORDER BY desde
                        """, String.class, negocioId, hasta, desde);
        List<EventoDeAuditoria> encontrados = new ArrayList<>();
        for (String url : urls) {
            byte[] contenido;
            try {
                contenido = almacen.leer(negocioId, url);
            } catch (AlmacenNoDisponibleException caido) {
                throw new ServicioNoDisponibleException(
                        "El archivo de la bitácora no responde: intenta en un momento");
            }
            for (Map<String, Object> e : lineas(contenido)) {
                if (negocioId.toString().equals(e.get("negocio_id")) && filtro.test(e)) {
                    encontrados.add(desdeArchivo(e));
                }
            }
        }
        return encontrados;
    }

    /** Lo vivo y lo archivado, sin repetir un evento que estuviera en los dos. */
    private static List<EventoDeAuditoria> juntar(List<EventoDeAuditoria> vivos, List<EventoDeAuditoria> archivados) {
        List<EventoDeAuditoria> todos = new ArrayList<>(vivos);
        var ids = vivos.stream().map(EventoDeAuditoria::id).collect(java.util.stream.Collectors.toSet());
        archivados.stream().filter(a -> !ids.contains(a.id())).forEach(todos::add);
        return todos;
    }

    private List<Map<String, Object>> lineas(byte[] gz) {
        List<Map<String, Object>> eventos = new ArrayList<>();
        try (BufferedReader lector = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(new ByteArrayInputStream(gz)), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                if (!linea.isBlank()) {
                    eventos.add(json.readValue(linea, new TypeReference<Map<String, Object>>() {}));
                }
            }
        } catch (IOException ilegible) {
            throw new UncheckedIOException("Un archivo de la bitácora está dañado", ilegible);
        }
        return eventos;
    }

    @SuppressWarnings("unchecked")
    private static EventoDeAuditoria desdeArchivo(Map<String, Object> e) {
        return new EventoDeAuditoria(UUID.fromString((String) e.get("id")), uuid(e.get("usuario_id")),
                (String) e.get("servicio"), (String) e.get("entidad_tipo"), uuid(e.get("entidad_id")),
                (String) e.get("accion"), (String) e.get("trace_id"), (String) e.get("resultado"),
                OffsetDateTime.parse((String) e.get("ocurrido_en")), (Map<String, Object>) e.get("cambios"), true);
    }

    private static UUID uuid(Object valor) {
        return valor == null ? null : UUID.fromString(Objects.toString(valor));
    }

    private EventoDeAuditoria fila(ResultSet rs, int i) throws SQLException {
        return new EventoDeAuditoria(UUID.fromString(rs.getString("id")),
                rs.getString("usuario_id") == null ? null : UUID.fromString(rs.getString("usuario_id")),
                rs.getString("servicio"), rs.getString("entidad_tipo"),
                rs.getString("entidad_id") == null ? null : UUID.fromString(rs.getString("entidad_id")),
                rs.getString("accion"), rs.getString("trace_id"), rs.getString("resultado"),
                rs.getObject("ocurrido_en", OffsetDateTime.class), cambios(rs.getString("cambios")), false);
    }

    private Map<String, Object> cambios(String texto) {
        if (texto == null) {
            return null;
        }
        try {
            return json.readValue(texto, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException ilegible) {
            throw new IllegalStateException("La bitácora tiene un detalle ilegible", ilegible);
        }
    }
}
