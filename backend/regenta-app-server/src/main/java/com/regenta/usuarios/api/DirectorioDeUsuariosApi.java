package com.regenta.usuarios.api;

import java.util.List;
import java.util.UUID;

/**
 * API pública expuesta por el módulo de usuarios para consultas internas
 * de otros módulos (ej. servicio-alertas).
 */
public interface DirectorioDeUsuariosApi {
    List<UUID> usuariosConRol(UUID negocioId, String rol);
}
