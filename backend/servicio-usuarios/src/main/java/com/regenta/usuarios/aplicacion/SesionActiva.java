package com.regenta.usuarios.aplicacion;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Un dispositivo con sesion abierta. Lo que la app lista para poder cerrarlo. */
public record SesionActiva(
        UUID id,
        String dispositivoId,
        String plataforma,
        OffsetDateTime emitidoEn,
        OffsetDateTime expiraEn) {
}
