package com.regenta.usuarios.aplicacion;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.negocio.FijadorDeNegocio;
import com.regenta.usuarios.domain.RefreshToken;
import com.regenta.usuarios.infra.RefreshTokenRepositorio;

/**
 * Corta una sesion en su propia transaccion.
 *
 * <p>Va aparte por la misma razon que {@link RegistroDeIntentos}: cuando se
 * detecta el reuso de un refresh token, el metodo que lo detecta termina
 * lanzando 401. Si la revocacion viviera en esa transaccion se iria con el
 * rollback, y el token robado seguiria sirviendo -- que es exactamente lo que
 * HU-014 viene a evitar.
 */
@Service
public class RevocadorDeSesiones {

    private final RefreshTokenRepositorio refrescos;
    private final FijadorDeNegocio fijador;
    private final Clock reloj;

    public RevocadorDeSesiones(RefreshTokenRepositorio refrescos, FijadorDeNegocio fijador,
            Clock reloj) {
        this.refrescos = refrescos;
        this.fijador = fijador;
        this.reloj = reloj;
    }

    /**
     * Revoca todo lo vivo de esa sesion. Con dispositivo, solo el suyo; sin
     * dispositivo, todo lo del usuario, que es lo prudente cuando no se sabe
     * por donde entro el que no debia.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int revocarLaSesion(UUID negocioId, UUID usuarioId, String dispositivoId) {
        fijador.fijar(negocioId);
        List<RefreshToken> vivos = dispositivoId == null || dispositivoId.isBlank()
                ? refrescos.findByUsuarioIdAndRevocadoEnIsNull(usuarioId)
                : refrescos.findByUsuarioIdAndDispositivoIdAndRevocadoEnIsNull(usuarioId,
                        dispositivoId);
        OffsetDateTime ahora = OffsetDateTime.now(reloj);
        vivos.forEach(token -> token.revocar(ahora));
        refrescos.saveAll(vivos);
        return vivos.size();
    }
}
