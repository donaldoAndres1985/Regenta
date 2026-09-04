package com.regenta.usuarios.aplicacion;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * El token va en claro una sola vez, aqui. En la base solo queda su hash, asi
 * que si se pierde no se recupera: se invita otra vez.
 */
public record InvitacionEmitida(
        UUID invitacionId,
        UUID usuarioId,
        String email,
        String token,
        OffsetDateTime expiraEn) {
}
