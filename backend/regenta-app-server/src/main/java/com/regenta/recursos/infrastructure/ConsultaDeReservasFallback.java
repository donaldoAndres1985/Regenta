package com.regenta.recursos.infrastructure;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import com.regenta.recursos.api.ConsultaDeReservasDeRecurso;
import com.regenta.recursos.api.ReservaAfectada;

/**
 * Fallback para {@link ConsultaDeReservasDeRecurso} cuando el módulo reservas
 * aún no provee la implementación directa en el contexto.
 */
@Component
@ConditionalOnMissingBean(ConsultaDeReservasDeRecurso.class)
public class ConsultaDeReservasFallback implements ConsultaDeReservasDeRecurso {

    @Override
    public boolean tieneReservasFuturas(UUID negocioId, UUID recursoId) {
        return false;
    }

    @Override
    public List<ReservaAfectada> reservasConfirmadasEnPeriodo(UUID negocioId, UUID recursoId,
            OffsetDateTime desde, OffsetDateTime hasta) {
        return List.of();
    }
}
