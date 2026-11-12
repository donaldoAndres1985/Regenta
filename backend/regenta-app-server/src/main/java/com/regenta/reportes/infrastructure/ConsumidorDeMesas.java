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
 * Mantiene {@code dim_mesa} (HU-134): qué código y qué zona tiene cada mesa.
 * {@code hechos_comanda.mesa_codigo} lleva NULL desde V1 porque nada publicaba
 * el catálogo de mesas, igual que le pasaba a {@code dim_recurso} antes de
 * HU-099.
 *
 * <p>Los tres eventos del catálogo traen la mesa completa, así que el
 * consumidor hace siempre el mismo upsert y no le importa cuál llegó primero.
 */
@Component
public class ConsumidorDeMesas {

    private final InboxIdempotente inbox;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public ConsumidorDeMesas(InboxIdempotente inbox, JdbcTemplate jdbc, ObjectMapper json) {
        this.inbox = inbox;
        this.jdbc = jdbc;
        this.json = json;
    }

        private static final Set<String> EVENTOS_SOPORTADOS = Set.of("mesa_creada", "mesa_actualizada", "mesa_eliminada");

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

        ContextoDeNegocio.en(contextoDe(nId), () -> inbox.procesarUnaVez("reportes.ConsumidorDeMesas", mensajeId, nId, tipoEvento, cuerpo, payload -> guardar(nId, datos)));
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
                INSERT INTO reportes.dim_mesa (negocio_id, mesa_id, zona_id, zona_nombre, codigo,
                    nombre, capacidad, activa, actualizado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, now())
                ON CONFLICT (negocio_id, mesa_id) DO UPDATE SET
                    zona_id = EXCLUDED.zona_id,
                    zona_nombre = EXCLUDED.zona_nombre,
                    codigo = EXCLUDED.codigo,
                    nombre = EXCLUDED.nombre,
                    capacidad = EXCLUDED.capacidad,
                    activa = EXCLUDED.activa,
                    actualizado_en = now()
                """,
                negocioId, uuid(datos.get("mesa_id")), uuid(datos.get("zona_id")),
                texto(datos.get("zona_nombre")), texto(datos.get("codigo")), texto(datos.get("nombre")),
                entero(datos.get("capacidad")), verdadero(datos.get("activa")));
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "COMANDA", Set.of(), Set.of("REPORTES"),
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
            throw new IllegalArgumentException("No se pudo leer el evento del catálogo de mesas", e);
        }
    }
}
