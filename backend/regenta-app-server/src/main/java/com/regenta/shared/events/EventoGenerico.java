package com.regenta.shared.events;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EventoGenerico(
        UUID negocioId,
        String agregadoTipo,
        UUID agregadoId,
        String tipoEvento,
        Object payload,
        String traceId,
        OffsetDateTime ocurridoEn
) implements EventoDeDominio {
}
