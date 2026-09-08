package com.regenta.alertas.aplicacion;

import java.util.UUID;

import com.regenta.alertas.domain.DispositivoPush;

/** Un dispositivo push del usuario. */
public record DispositivoDelUsuario(UUID id, String plataforma, boolean activo) {

    static DispositivoDelUsuario de(DispositivoPush d) {
        return new DispositivoDelUsuario(d.getId(), d.getPlataforma().name(), d.isActivo());
    }
}
