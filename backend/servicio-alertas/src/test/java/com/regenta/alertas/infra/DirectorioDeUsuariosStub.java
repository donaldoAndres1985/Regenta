package com.regenta.alertas.infra;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.regenta.alertas.aplicacion.DirectorioDeUsuarios;

/**
 * Stub del directorio de usuarios (HU-092 criterio 5). Devuelve lo que se le
 * cargue con {@link #cargar}. Reemplaza a {@link DirectorioDeUsuariosHttp} en los tests; en
 * producción se le pregunta a {@code servicio-usuarios} (HU-138).
 */
@Component
@Primary
public class DirectorioDeUsuariosStub implements DirectorioDeUsuarios {

    private final Map<String, List<UUID>> porNegocioYRol = new ConcurrentHashMap<>();

    public void cargar(UUID negocioId, String rol, List<UUID> usuarios) {
        porNegocioYRol.put(clave(negocioId, rol), List.copyOf(usuarios));
    }

    public void reiniciar() {
        porNegocioYRol.clear();
    }

    @Override
    public List<UUID> usuariosConRol(UUID negocioId, String rol) {
        return new ArrayList<>(porNegocioYRol.getOrDefault(clave(negocioId, rol), List.of()));
    }

    private static String clave(UUID negocioId, String rol) {
        return negocioId + "|" + rol;
    }
}
