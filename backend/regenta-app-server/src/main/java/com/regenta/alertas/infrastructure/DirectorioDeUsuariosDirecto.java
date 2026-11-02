package com.regenta.alertas.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.regenta.alertas.application.DirectorioDeUsuarios;
import com.regenta.usuarios.api.DirectorioDeUsuariosApi;

/**
 * Consulta de usuarios con rol invocando directamente el API pública del módulo usuarios.
 */
@Component
public class DirectorioDeUsuariosDirecto implements DirectorioDeUsuarios {

    private final DirectorioDeUsuariosApi usuariosApi;

    public DirectorioDeUsuariosDirecto(DirectorioDeUsuariosApi usuariosApi) {
        this.usuariosApi = usuariosApi;
    }

    @Override
    public List<UUID> usuariosConRol(UUID negocioId, String rol) {
        var usuarios = usuariosApi.usuariosConRol(negocioId, rol);
        return usuarios == null ? List.of() : usuarios;
    }
}
