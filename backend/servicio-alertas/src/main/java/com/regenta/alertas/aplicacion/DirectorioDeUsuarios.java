package com.regenta.alertas.aplicacion;

import java.util.List;
import java.util.UUID;

/**
 * Resuelve un rol a los usuarios que lo tienen en un negocio (HU-092 criterio
 * 5). La implementación real consulta a {@code servicio-usuarios}; hasta que esa
 * integración exista, un stub configurable la reemplaza.
 */
public interface DirectorioDeUsuarios {

    List<UUID> usuariosConRol(UUID negocioId, String rol);
}
