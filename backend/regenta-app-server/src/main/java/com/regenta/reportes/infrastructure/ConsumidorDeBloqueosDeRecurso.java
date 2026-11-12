package com.regenta.reportes.infrastructure;

import java.time.LocalDate;
import java.time.OffsetDateTime;
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
import com.regenta.reportes.application.AgregadorDeOcupacion;

/**
 * Reparte un bloqueo de recurso noche por noche en {@code bloqueos_recurso_dia}
 * y recalcula el denominador de la ocupación (HU-136). Hasta HU-099 el
 * denominador era «los recursos activos del tipo»: una habitación en obra
 * seguía contando y el hotel salía peor de lo que fue.
 *
 * <p>Al levantar un bloqueo solo se borran las noches de <b>hoy en adelante</b>
 * (criterio 4). Las que ya pasaron no se reescriben: la habitación sí estuvo
 * fuera de servicio esas noches, y un reporte de ayer no cambia porque hoy se
 * levante el bloqueo.
 */
@Component
public class ConsumidorDeBloqueosDeRecurso {

    private final InboxIdempotente inbox;
    private final AgregadorDeOcupacion ocupacion;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public ConsumidorDeBloqueosDeRecurso(InboxIdempotente inbox, AgregadorDeOcupacion ocupacion,
            JdbcTemplate jdbc, ObjectMapper json) {
        this.inbox = inbox;
        this.ocupacion = ocupacion;
        this.jdbc = jdbc;
        this.json = json;
    }

        private static final Set<String> EVENTOS_SOPORTADOS = Set.of("bloqueo_recurso_creado", "bloqueo_recurso_levantado");

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

        boolean levantado = "bloqueo_recurso_levantado".equals(tipoEvento); ContextoDeNegocio.en(contextoDe(nId), () -> inbox.procesarUnaVez("reportes.ConsumidorDeBloqueosDeRecurso", mensajeId, nId, tipoEvento, cuerpo, payload -> procesar(nId, levantado, datos)));
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

    private void procesar(UUID negocioId, boolean levantado, Map<String, Object> datos) {
        UUID bloqueoId = uuid(datos.get("bloqueo_id"));
        UUID recursoId = uuid(datos.get("recurso_id"));
        UUID tipoRecursoId = uuid(datos.get("tipo_recurso_id"));
        UUID sucursalId = uuid(datos.get("sucursal_id"));
        LocalDate desde = fecha(datos.get("desde"));
        LocalDate hasta = fecha(datos.get("hasta"));
        String motivo = datos.get("motivo") == null ? "OTRO" : datos.get("motivo").toString();

        if (tipoRecursoId == null || desde == null || hasta == null || !desde.isBefore(hasta)) {
            return;
        }

        if (levantado) {
            LocalDate hoy = LocalDate.now();
            LocalDate primeraQueVuelve = desde.isBefore(hoy) ? hoy : desde;
            jdbc.update("DELETE FROM reportes.bloqueos_recurso_dia WHERE negocio_id = ? "
                    + "AND bloqueo_id = ? AND fecha >= ?", negocioId, bloqueoId, primeraQueVuelve);
            recalcular(negocioId, tipoRecursoId, sucursalId, primeraQueVuelve, hasta);
            return;
        }

        for (LocalDate noche = desde; noche.isBefore(hasta); noche = noche.plusDays(1)) {
            jdbc.update("""
                    INSERT INTO reportes.bloqueos_recurso_dia (negocio_id, fecha, recurso_id,
                        tipo_recurso_id, sucursal_id, bloqueo_id, motivo)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (negocio_id, fecha, recurso_id) DO UPDATE SET
                        bloqueo_id = EXCLUDED.bloqueo_id,
                        motivo = EXCLUDED.motivo
                    """,
                    negocioId, noche, recursoId, tipoRecursoId, sucursalId, bloqueoId, motivo);
        }
        recalcular(negocioId, tipoRecursoId, sucursalId, desde, hasta);
    }

    private void recalcular(UUID negocioId, UUID tipoRecursoId, UUID sucursalId, LocalDate desde,
            LocalDate hasta) {
        for (LocalDate noche = desde; noche.isBefore(hasta); noche = noche.plusDays(1)) {
            ocupacion.recalcularNoche(negocioId, tipoRecursoId, sucursalId, noche);
        }
    }

    private static UUID uuid(Object valor) {
        return valor == null ? null : UUID.fromString(valor.toString());
    }

    /**
     * El periodo viene con su offset, tal como lo fijó quien bloqueó. Se toma
     * el día como vino, sin reinterpretarlo en la zona del servicio: correrlo
     * movería la noche de entrada un día entero, igual que en las estancias.
     */
    private static LocalDate fecha(Object valor) {
        return valor == null ? null : OffsetDateTime.parse(valor.toString()).toLocalDate();
    }

    private Map<String, Object> leer(String cuerpo) {
        try {
                        Map<String, Object> mapa = json.readValue(cuerpo, new TypeReference<Map<String, Object>>() {});
            return mapa;
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el evento de bloqueo", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "RESERVA", Set.of(), Set.of("REPORTES"), Set.of(),
                Set.of());
    }
}
