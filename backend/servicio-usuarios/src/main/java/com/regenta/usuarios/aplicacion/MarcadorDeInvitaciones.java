package com.regenta.usuarios.aplicacion;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.negocio.FijadorDeNegocio;
import com.regenta.usuarios.domain.Invitacion;
import com.regenta.usuarios.infra.InvitacionRepositorio;

/**
 * Marca una invitacion vencida en su propia transaccion.
 *
 * <p>Va aparte por la misma razon que {@link RegistroDeIntentos}: aceptar una
 * invitacion vencida termina lanzando 410, y si el cambio de estado viviera en
 * esa misma transaccion se iria con el rollback. La invitacion se quedaria
 * PENDIENTE para siempre -- justo lo contrario del criterio 4 de HU-015.
 */
@Service
public class MarcadorDeInvitaciones {

    private final InvitacionRepositorio invitaciones;
    private final FijadorDeNegocio fijador;

    public MarcadorDeInvitaciones(InvitacionRepositorio invitaciones, FijadorDeNegocio fijador) {
        this.invitaciones = invitaciones;
        this.fijador = fijador;
    }

    /** Devuelve true si esta llamada fue la que la dejo EXPIRADA. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean marcarVencida(UUID negocioId, UUID invitacionId) {
        fijador.fijar(negocioId);
        return invitaciones.findById(invitacionId)
                .filter(Invitacion::estaPendiente)
                .map(invitacion -> {
                    invitacion.marcarVencida();
                    invitaciones.save(invitacion);
                    return true;
                })
                .orElse(false);
    }
}
