package com.regenta.auditoria.infra;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.auditoria.aplicacion.AccionDeAuditoria;
import com.regenta.comun.eventos.InboxIdempotente;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.DatosDelNegocio;

/**
 * HU-101 criterio 1: la bitácora de TODO lo que pasa en cualquier servicio,
 * sin instrumentar los otros catorce uno por uno. Se logra escuchando el
 * exchange entero ({@code key = "#"}, el mismo patrón que ya usa la cola de
 * muertos en {@code EventosAutoConfiguracion}) y leyendo las cabeceras que
 * {@code PublicadorDeOutbox} ya pone en cada mensaje: {@code negocio_id},
 * {@code agregado_tipo}, {@code agregado_id}, {@code trace_id} y
 * {@code servicio_origen}.
 *
 * <p>Límite conocido (criterio 3): sin tocar cada servicio para que declare
 * un diff campo a campo, esta bitácora guarda el payload resultante del
 * evento como {@code datos_despues}, no un {@code cambios} antes/después.
 *
 * <p>El mismo evento también alimenta {@code cambios_servidor} (HU-104): el
 * watermark del que sale la descarga incremental de la app.
 */
@Component
public class AuditorDeEventos {

    private final InboxIdempotente inbox;
    private final EscritorDeAuditoria escritor;
    private final EscritorDeCambios cambios;
    private final ObjectMapper json;

    public AuditorDeEventos(InboxIdempotente inbox, EscritorDeAuditoria escritor, EscritorDeCambios cambios,
            ObjectMapper json) {
        this.inbox = inbox;
        this.escritor = escritor;
        this.cambios = cambios;
        this.json = json;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "auditoria.todos-los-eventos", durable = "true"),
            exchange = @Exchange(name = "${regenta.eventos.exchange:regenta.eventos}",
                    type = ExchangeTypes.TOPIC, durable = "true"),
            key = {"#"}))
    public void recibir(Message mensaje) {
        MessageProperties props = mensaje.getMessageProperties();
        UUID mensajeId = UUID.fromString(props.getMessageId());
        String tipoEvento = props.getReceivedRoutingKey();
        Map<String, Object> headers = props.getHeaders();

        Object negocioHeader = headers.get("negocio_id");
        if (negocioHeader == null) {
            return; // no es un evento de negocio: nada que auditar por negocio
        }
        UUID negocioId = UUID.fromString(negocioHeader.toString());
        String cuerpo = new String(mensaje.getBody(), StandardCharsets.UTF_8);

        ContextoDeNegocio.en(contextoDe(negocioId), () -> inbox.procesarUnaVez(mensajeId, negocioId,
                tipoEvento, cuerpo, payload -> procesar(mensajeId, negocioId, tipoEvento, headers, cuerpo)));
    }

    private void procesar(UUID mensajeId, UUID negocioId, String tipoEvento, Map<String, Object> headers,
            String cuerpo) {
        String servicio = texto(headers.get("servicio_origen"));
        String entidadTipo = texto(headers.get("agregado_tipo"));
        UUID entidadId = uuid(headers.get("agregado_id"));
        String traceId = texto(headers.get("trace_id"));
        UUID usuarioId = usuarioDesdePayload(cuerpo);
        String accion = AccionDeAuditoria.desde(tipoEvento);
        OffsetDateTime ahora = OffsetDateTime.now();

        escritor.insertar(mensajeId, negocioId, usuarioId,
                servicio == null || servicio.isBlank() ? "desconocido" : servicio,
                entidadTipo == null || entidadTipo.isBlank() ? "Desconocida" : entidadTipo, entidadId,
                accion, cuerpo, traceId, ahora);

        // HU-104: el mismo evento alimenta el watermark de descarga incremental. Va en
        // la misma transacción del Inbox de arriba: un segundo @RabbitListener con
        // "#" en este servicio chocaría por el mismo mensaje contra el mismo Inbox.
        if (entidadId != null) {
            cambios.insertar(negocioId, entidadTipo == null || entidadTipo.isBlank() ? "Desconocida"
                    : entidadTipo, entidadId, operacionDeCambio(accion), cuerpo, ahora);
        }
    }

    private static String operacionDeCambio(String accion) {
        return switch (accion) {
            case "CREAR" -> "CREAR";
            case "ELIMINAR" -> "ELIMINAR";
            default -> "ACTUALIZAR";
        };
    }

    private UUID usuarioDesdePayload(String cuerpo) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> datos = json.readValue(cuerpo, Map.class);
            return uuid(datos.get("usuario_id"));
        } catch (Exception e) {
            return null;
        }
    }

    private static String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }

    private static UUID uuid(Object valor) {
        if (valor == null) {
            return null;
        }
        try {
            return UUID.fromString(valor.toString());
        } catch (IllegalArgumentException noEsUuid) {
            return null;
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "VENTA_DIRECTA", Set.of(), Set.of("AUDITORIA"),
                Set.of(), Set.of());
    }
}
