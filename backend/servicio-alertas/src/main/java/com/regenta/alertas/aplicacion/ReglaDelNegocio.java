package com.regenta.alertas.aplicacion;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.regenta.alertas.domain.ReglaAlerta;

/** Una regla de alerta como la ve el administrador (HU-092). */
public record ReglaDelNegocio(
        UUID id,
        String tipoCodigo,
        String nombre,
        UUID sucursalId,
        Map<String, Object> condicion,
        String severidad,
        List<String> canales,
        List<String> destinatariosRoles,
        List<UUID> destinatariosUsuarios,
        String frecuencia,
        LocalTime horaEnvio,
        int silenciarHoras,
        boolean activa) {

    static ReglaDelNegocio de(ReglaAlerta r) {
        return new ReglaDelNegocio(r.getId(), r.getTipoCodigo(), r.getNombre(), r.getSucursalId(),
                r.getCondicion(), r.getSeveridad().name(), r.getCanales(),
                r.getDestinatariosRoles(), r.getDestinatariosUsuarios(),
                r.getFrecuencia().name(), r.getHoraEnvio(), r.getSilenciarHoras(), r.isActiva());
    }
}
