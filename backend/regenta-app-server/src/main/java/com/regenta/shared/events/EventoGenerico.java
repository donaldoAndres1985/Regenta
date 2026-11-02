package com.regenta.shared.events;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EventoGenerico(
        UUID id,
        UUID negocioId,
        String agregadoTipo,
        UUID agregadoId,
        String tipoEvento,
        Object payload,
        String traceId,
        OffsetDateTime ocurridoEn
) implements EventoDeDominio {

    public EventoGenerico {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (ocurridoEn == null) {
            ocurridoEn = OffsetDateTime.now();
        }
    }

    public EventoGenerico(UUID negocioId, String agregadoTipo, UUID agregadoId,
                          String tipoEvento, Object payload, String traceId) {
        this(UUID.randomUUID(), negocioId, agregadoTipo, agregadoId, tipoEvento, payload, traceId, OffsetDateTime.now());
    }
}
