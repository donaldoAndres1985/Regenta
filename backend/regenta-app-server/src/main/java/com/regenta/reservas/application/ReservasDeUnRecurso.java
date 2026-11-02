package com.regenta.reservas.application;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.shared.web.ReglaDeNegocioException;
import com.regenta.shared.security.RequierePermiso;
import com.regenta.reservas.infrastructure.ConsultaDeOcupacion;

/**
 * Las reservas de un recurso, para servicio-recursos (HU-138): si tiene reservas
 * por delante antes de borrarlo (HU-065 criterio 3) y quÃ© reservas confirmadas
 * pisa un bloqueo nuevo (HU-067 criterio 3). La RLS acota todo al negocio de
 * quien pregunta.
 */
@Service
public class ReservasDeUnRecurso {

    private final ConsultaDeOcupacion ocupacion;

    public ReservasDeUnRecurso(ConsultaDeOcupacion ocupacion) {
        this.ocupacion = ocupacion;
    }

    /**
     * Las que se solapan con {@code [desde, hasta)}; sin {@code hasta}, todas
     * las que terminan despuÃ©s de {@code desde}. Con {@code soloConfirmadas},
     * solo las CONFIRMADA; si no, todas las que ocupan el recurso.
     */
    @Transactional(readOnly = true)
    @RequierePermiso("RESERVAS_RESERVA_VER")
    public List<ReservaDeUnRecurso> delRecurso(UUID recursoId, OffsetDateTime desde,
            OffsetDateTime hasta, boolean soloConfirmadas) {
        if (desde == null || (hasta != null && !hasta.isAfter(desde))) {
            throw new ReglaDeNegocioException("El periodo necesita un desde y un hasta posterior");
        }
        return ocupacion.reservasDeRecurso(recursoId, desde, hasta, soloConfirmadas);
    }
}

