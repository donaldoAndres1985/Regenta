package com.regenta.usuarios.aplicacion;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.negocio.FijadorDeNegocio;
import com.regenta.usuarios.domain.Usuario;
import com.regenta.usuarios.infra.UsuarioRepositorio;

/**
 * Cuenta los intentos fallidos en su propia transaccion.
 *
 * <p>Tiene que ser aparte: el login termina lanzando 401, y si el contador
 * viviera en esa misma transaccion se iria con el rollback. Es decir, nunca
 * bloquearia una cuenta -- justo lo contrario de lo que pide HU-013.
 */
@Service
public class RegistroDeIntentos {

    private final UsuarioRepositorio usuarios;
    private final FijadorDeNegocio fijador;
    private final Clock reloj;

    public RegistroDeIntentos(UsuarioRepositorio usuarios, FijadorDeNegocio fijador, Clock reloj) {
        this.usuarios = usuarios;
        this.fijador = fijador;
        this.reloj = reloj;
    }

    /** Devuelve true si este intento fue el que bloqueo la cuenta. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean fallo(UUID negocioId, UUID usuarioId) {
        fijador.fijar(negocioId);
        Usuario usuario = usuarios.findById(usuarioId).orElse(null);
        if (usuario == null) {
            return false;
        }
        boolean bloqueado = usuario.fallo(OffsetDateTime.now(reloj));
        usuarios.save(usuario);
        return bloqueado;
    }
}
