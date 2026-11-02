package com.regenta.shared.events;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publicador central de eventos en proceso para Spring Modulith.
 *
 * <p>Reemplaza la publicación asíncrona a RabbitMQ por publicación de eventos
 * de dominio de Spring en memoria, respaldados transaccionalmente por el
 * Event Publication Registry de Spring Modulith.
 */
@Component
public class RegistroDeEventos {

    private final ApplicationEventPublisher publisher;

    public RegistroDeEventos(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public EventoGenerico registrar(UUID negocioId, String agregadoTipo, UUID agregadoId,
                                    String tipoEvento, Object payload) {
        return registrar(negocioId, agregadoTipo, agregadoId, tipoEvento, payload, null);
    }

    public EventoGenerico registrar(UUID negocioId, String agregadoTipo, UUID agregadoId,
                                    String tipoEvento, Object payload, String traceId) {
        EventoGenerico evento = new EventoGenerico(negocioId, agregadoTipo, agregadoId,
                tipoEvento, payload, traceId, OffsetDateTime.now());
        publisher.publishEvent(evento);
        return evento;
    }
}
