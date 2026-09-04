package com.regenta.comun.eventos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Por donde un servicio publica un evento: escribiendolo en su propia base.
 *
 * <p>{@code MANDATORY} a proposito. Registrar un evento fuera de la transaccion del
 * agregado no tiene sentido —seria exactamente el problema que el Outbox viene a
 * resolver—, asi que en vez de abrir una transaccion nueva, falla.
 */
@Service
public class RegistroDeEventos {

    private final OutboxRepositorio outbox;
    private final ObjectMapper json;

    public RegistroDeEventos(OutboxRepositorio outbox, ObjectMapper json) {
        this.outbox = outbox;
        this.json = json;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvento registrar(UUID negocioId, String agregadoTipo, UUID agregadoId,
                                  String tipoEvento, Object payload) {
        return registrar(negocioId, agregadoTipo, agregadoId, tipoEvento, aJson(payload), null);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvento registrar(UUID negocioId, String agregadoTipo, UUID agregadoId,
                                  String tipoEvento, String payloadJson, String traceId) {
        OutboxEvento evento = new OutboxEvento(UUID.randomUUID(), negocioId, agregadoTipo,
                agregadoId, tipoEvento, payloadJson, traceId);
        return outbox.save(evento);
    }

    private String aJson(Object payload) {
        if (payload == null) {
            return "{}";
        }
        if (payload instanceof String texto) {
            return texto;
        }
        try {
            return json.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("El payload del evento no se puede serializar", e);
        }
    }
}
