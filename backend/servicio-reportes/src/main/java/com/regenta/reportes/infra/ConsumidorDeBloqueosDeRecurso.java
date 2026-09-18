package com.regenta.reportes.infra;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.comun.eventos.InboxIdempotente;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.DatosDelNegocio;
import com.regenta.reportes.aplicacion.AgregadorDeOcupacion;

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

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "reportes.bloqueos-de-recurso", durable = "true"),
            exchange = @Exchange(name = "${regenta.eventos.exchange:regenta.eventos}",
                    type = ExchangeTypes.TOPIC, durable = "true"),
            key = {"bloqueo_recurso_creado", "bloqueo_recurso_levantado"}))
    public void recibir(Message mensaje) {
        var props = mensaje.getMessageProperties();
        UUID mensajeId = UUID.fromString(props.getMessageId());
        String tipoEvento = props.getReceivedRoutingKey();
        String cuerpo = new String(mensaje.getBody(), StandardCharsets.UTF_8);
        Map<String, Object> datos = leer(cuerpo);

        Object negocio = datos.get("negocio_id");
        if (negocio == null) {
            return;
        }
        UUID negocioId = UUID.fromString(negocio.toString());
        boolean levantado = "bloqueo_recurso_levantado".equals(tipoEvento);

        ContextoDeNegocio.en(contextoDe(negocioId), () -> inbox.procesarUnaVez(mensajeId, negocioId,
                tipoEvento, cuerpo, payload -> procesar(negocioId, levantado, datos)));
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
            @SuppressWarnings("unchecked")
            Map<String, Object> mapa = json.readValue(cuerpo, Map.class);
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
