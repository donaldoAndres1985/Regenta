package com.regenta.reservas.infrastructure;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.regenta.recursos.api.ConsultaDeReservasDeRecurso;
import com.regenta.recursos.api.ReservaAfectada;
import com.regenta.reservas.application.ReservaDeUnRecurso;
import com.regenta.reservas.application.ReservasDeUnRecurso;

/**
 * Implementación del SPI requerido por recursos para verificar reservas futuras y afectaciones.
 */
@Component
public class ConsultaDeReservasAdaptador implements ConsultaDeReservasDeRecurso {

    private final ReservasDeUnRecurso reservasDeUnRecurso;

    public ConsultaDeReservasAdaptador(ReservasDeUnRecurso reservasDeUnRecurso) {
        this.reservasDeUnRecurso = reservasDeUnRecurso;
    }

    @Override
    public boolean tieneReservasFuturas(UUID negocioId, UUID recursoId) {
        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);
        List<ReservaDeUnRecurso> reservas = reservasDeUnRecurso.delRecurso(recursoId, ahora, null, false);
        return !reservas.isEmpty();
    }

    @Override
    public List<ReservaAfectada> reservasConfirmadasEnPeriodo(UUID negocioId, UUID recursoId,
            OffsetDateTime desde, OffsetDateTime hasta) {
        List<ReservaDeUnRecurso> reservas = reservasDeUnRecurso.delRecurso(recursoId, desde, hasta, true);
        return reservas.stream()
                .map(r -> new ReservaAfectada(r.id(), r.numero(), r.desde(), r.hasta(), null))
                .toList();
    }
}
