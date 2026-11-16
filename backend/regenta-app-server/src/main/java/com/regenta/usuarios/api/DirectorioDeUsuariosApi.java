package com.regenta.usuarios.api;

import java.util.List;
import java.util.UUID;

/**
 * API pública expuesta por el módulo de usuarios para consultas internas
 * de otros módulos (ej. servicio-alertas).
 */
public interface DirectorioDeUsuariosApi {
    List<UUID> usuariosConRol(UUID negocioId, String rol);

    /** Nombre y apellido de cada usuario del negocio (HU-142: la tirilla de cierre). */
    java.util.Map<UUID, String> nombresDe(UUID negocioId, java.util.Collection<UUID> usuarios);
}
