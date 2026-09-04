package com.regenta.comun.eventos;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saca del Outbox lo que este pendiente y lo publica.
 *
 * <p>Corre en su propia transaccion, aparte de la que guardo el agregado. Si el broker
 * esta caido, el evento se queda PENDIENTE y se reintenta en la siguiente pasada: la
 * operacion de negocio ya termino bien y nadie se entera del problema del bus.
 *
 * <p>Cada mensaje sale con {@code message-id} igual al id del evento. Ese id es el que
 * el consumidor usa como clave del Inbox para no procesarlo dos veces.
 */
public class PublicadorDeOutbox {

    private static final Logger log = LoggerFactory.getLogger(PublicadorDeOutbox.class);

    private final OutboxRepositorio outbox;
    private final RabbitTemplate rabbit;
    private final PropiedadesEventos propiedades;

    public PublicadorDeOutbox(OutboxRepositorio outbox, RabbitTemplate rabbit,
                              PropiedadesEventos propiedades) {
        this.outbox = outbox;
        this.rabbit = rabbit;
        this.propiedades = propiedades;
    }

    /** Devuelve cuantos publico. Se llama sola por schedule, y a mano en los tests. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int publicarPendientes() {
        List<OutboxEvento> pendientes = outbox.tomarPendientes(propiedades.getLote());
        int publicados = 0;
        for (OutboxEvento evento : pendientes) {
            try {
                rabbit.send(propiedades.getExchange(), evento.getTipoEvento(), aMensaje(evento));
                evento.marcarPublicado();
                publicados++;
            } catch (Exception e) {
                boolean agotado = evento.marcarIntentoFallido(e.getMessage(), propiedades.getMaximoIntentos());
                if (agotado) {
                    log.error("Evento {} ({}) agoto {} intentos: va a la cola muerta",
                            evento.getId(), evento.getTipoEvento(), propiedades.getMaximoIntentos());
                    aLaColaMuerta(evento);
                } else {
                    log.warn("Fallo al publicar el evento {} ({}), intento {}: {}",
                            evento.getId(), evento.getTipoEvento(), evento.getIntentos(), e.getMessage());
                }
            }
        }
        outbox.saveAll(pendientes);
        return publicados;
    }

    private void aLaColaMuerta(OutboxEvento evento) {
        try {
            rabbit.send(propiedades.getExchangeMuertos(), evento.getTipoEvento(), aMensaje(evento));
        } catch (Exception e) {
            // Si tampoco se puede escribir en la cola muerta, el evento igual queda
            // FALLIDO en la base con su ultimo error: no se pierde el rastro.
            log.error("El evento {} no se pudo enrutar a la cola muerta: {}", evento.getId(), e.getMessage());
        }
    }

    private Message aMensaje(OutboxEvento evento) {
        MessageProperties propiedades = new MessageProperties();
        propiedades.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        propiedades.setContentEncoding(StandardCharsets.UTF_8.name());
        propiedades.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        // El message-id es el id del outbox: con ese id el consumidor descarta
        // los repetidos en su inbox. Si se pierde, la idempotencia no tiene de
        // donde agarrarse.
        propiedades.setMessageId(evento.getId().toString());
        propiedades.setHeader("negocio_id", evento.getNegocioId().toString());
        propiedades.setHeader("tipo_evento", evento.getTipoEvento());
        propiedades.setHeader("agregado_tipo", evento.getAgregadoTipo());
        propiedades.setHeader("agregado_id", evento.getAgregadoId().toString());
        if (evento.getTraceId() != null) {
            propiedades.setHeader("trace_id", evento.getTraceId());
        }
        return new Message(evento.getPayload().getBytes(StandardCharsets.UTF_8), propiedades);
    }
}
