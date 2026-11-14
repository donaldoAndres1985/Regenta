package com.regenta.usuarios.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.usuarios.api.DirectorioDeUsuariosApi;

@Service
public class DirectorioDeUsuariosServicio implements DirectorioDeUsuariosApi {

    private final GestionDeUsuarios gestion;

    public DirectorioDeUsuariosServicio(GestionDeUsuarios gestion) {
        this.gestion = gestion;
    }

    @Override
    public List<UUID> usuariosConRol(UUID negocioId, String rol) {
        return ContextoDeNegocio.en(DatosDelNegocio.sistema(negocioId, "USUARIOS_USUARIO_VER"),
                () -> gestion.conRol(rol));
    }
}
