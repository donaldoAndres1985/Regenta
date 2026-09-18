package com.regenta.usuarios.aplicacion;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.eventos.RegistroDeEventos;

/**
 * Publica {@code login_fallido} en su propia transacción (HU-101, "el login
 * fallido también se audita").
 *
 * <p>Va aparte por la misma razón que {@link RegistroDeIntentos} y
 * {@link RevocadorDeSesiones}: {@link Autenticacion#entrar} termina lanzando
 * una excepción, y si el evento viviera en esa misma transacción se iría con
 * el rollback — nunca quedaría rastro del intento fallido, justo lo contrario
 * de lo que pide la bitácora de auditoría.
 */
@Service
public class RegistradorDeLoginFallido {

    private final RegistroDeEventos eventos;

    public RegistradorDeLoginFallido(RegistroDeEventos eventos) {
        this.eventos = eventos;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(UUID negocioId, UUID usuarioId, String email) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("negocio_id", negocioId.toString());
        datos.put("usuario_id", usuarioId.toString());
        datos.put("email", email);
        datos.put("motivo", "password_incorrecta");
        eventos.registrar(negocioId, "Usuario", usuarioId, "login_fallido", datos);
    }
}
