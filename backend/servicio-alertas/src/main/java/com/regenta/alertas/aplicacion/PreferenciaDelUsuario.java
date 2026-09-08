package com.regenta.alertas.aplicacion;

import java.time.LocalTime;
import java.util.List;

import com.regenta.alertas.domain.PreferenciaNotificacion;

/** La preferencia de notificación del usuario para un tipo de alerta. */
public record PreferenciaDelUsuario(
        String tipoCodigo,
        List<String> canales,
        boolean habilitada,
        LocalTime noMolestarDesde,
        LocalTime noMolestarHasta) {

    static PreferenciaDelUsuario de(PreferenciaNotificacion p) {
        return new PreferenciaDelUsuario(p.getTipoCodigo(), p.getCanales(), p.isHabilitada(),
                p.getNoMolestarDesde(), p.getNoMolestarHasta());
    }
}
