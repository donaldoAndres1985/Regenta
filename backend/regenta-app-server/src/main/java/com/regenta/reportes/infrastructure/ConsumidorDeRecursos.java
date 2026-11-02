package com.regenta.reportes.infrastructure;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.modulith.events.ApplicationModuleListener;
import com.regenta.shared.events.EventoGenerico;
import com.fasterxml.jackson.core.type.TypeReference;
import com.regenta.shared.events.InboxIdempotente;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

/**
 * Mantiene {@code dim_recurso}: cuántas habitaciones, canchas o consultorios
 * hay y cuáles siguen contando (HU-099). Es el denominador de la ocupación y
 * del RevPAR — sin él se sabe cuántas noches se vendieron, pero no sobre
 * cuántas.
 *
 * <p>Los tres eventos del catálogo traen el recurso completo, así que el
 * consumidor hace siempre el mismo upsert y no le importa cuál llegó: uno que
 * arranca a mitad de camino queda con la foto correcta sin reproducir la
 * historia.
 */
@Component
public class ConsumidorDeRecursos {

    private final InboxIdempotente inbox;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public ConsumidorDeRecursos(InboxIdempotente inbox, JdbcTemplate jdbc, ObjectMapper json) {
        this.inbox = inbox;
        this.jdbc = jdbc;
        this.json = json;
    }

        private static final Set<String> EVENTOS_SOPORTADOS = Set.of("recurso_creado", "recurso_actualizado", "recurso_eliminado");

    @ApplicationModuleListener
    public void recibir(EventoGenerico evento) {
        if (evento == null) {
            return;
        }
        String tipoEvento = evento.tipoEvento();
        if (!EVENTOS_SOPORTADOS.contains(tipoEvento)) {
            return;
        }

        UUID mensajeId = evento.id();
        String cuerpo = serializarPayload(evento.payload());
        Map<String, Object> datos = leer(cuerpo);

        Object negocio = datos.get("negocio_id");
        if (negocio == null && evento.negocioId() != null) {
            negocio = evento.negocioId();
        }
        if (negocio == null) {
            return;
        }
        UUID negocioId = UUID.fromString(negocio.toString());
        final UUID nId = negocioId;

        ContextoDeNegocio.en(contextoDe(nId), () -> inbox.procesarUnaVez(mensajeId, nId, tipoEvento, cuerpo, payload -> guardar(nId, datos)));
    }

    private String serializarPayload(Object payload) {
        if (payload == null) {
            return "{}";
        }
        if (payload instanceof String s) {
            return s;
        }
        try {
            return json.writeValueAsString(payload);
        } catch (Exception e) {
            return "{}";
        }
    }

    private void guardar(UUID negocioId, Map<String, Object> datos) {
        jdbc.update("""
                INSERT INTO reportes.dim_recurso (negocio_id, recurso_id, sucursal_id, tipo_recurso_id,
                    tipo_recurso_nombre, codigo, nombre, capacidad, estado, activo, disponible,
                    actualizado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())
                ON CONFLICT (negocio_id, recurso_id) DO UPDATE SET
                    sucursal_id = EXCLUDED.sucursal_id,
                    tipo_recurso_id = EXCLUDED.tipo_recurso_id,
                    tipo_recurso_nombre = EXCLUDED.tipo_recurso_nombre,
                    codigo = EXCLUDED.codigo,
                    nombre = EXCLUDED.nombre,
                    capacidad = EXCLUDED.capacidad,
                    estado = EXCLUDED.estado,
                    activo = EXCLUDED.activo,
                    disponible = EXCLUDED.disponible,
                    actualizado_en = now()
                """,
                negocioId, uuid(datos.get("recurso_id")), uuid(datos.get("sucursal_id")),
                uuid(datos.get("tipo_recurso_id")), texto(datos.get("tipo_recurso_nombre")),
                texto(datos.get("codigo")), texto(datos.get("nombre")), entero(datos.get("capacidad")),
                texto(datos.get("estado")), verdadero(datos.get("activo")),
                verdadero(datos.get("disponible")));
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "RESERVA", Set.of(), Set.of("REPORTES"),
                Set.of(), Set.of());
    }

    private static UUID uuid(Object valor) {
        return valor == null ? null : UUID.fromString(valor.toString());
    }

    private static String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }

    private static Integer entero(Object valor) {
        return valor == null ? null : Integer.valueOf(valor.toString());
    }

    private static boolean verdadero(Object valor) {
        return Boolean.parseBoolean(String.valueOf(valor));
    }

    private Map<String, Object> leer(String cuerpo) {
        try {
                        Map<String, Object> mapa = json.readValue(cuerpo, new TypeReference<Map<String, Object>>() {});
            return mapa;
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el evento del catálogo de recursos", e);
        }
    }
}
