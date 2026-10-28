package com.regenta.alertas.infra;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.regenta.alertas.aplicacion.DirectorioDeUsuarios;
import com.regenta.comun.errores.ServicioNoDisponibleException;
import com.regenta.comun.negocio.IdentidadSaliente;

/**
 * Le pregunta a servicio-usuarios quién tiene un rol, para entregarle una
 * alerta dirigida a ese rol (HU-092 criterio 5, HU-138 criterio 8). La alerta
 * la dispara un evento o un barrido, sin usuario: va como SISTEMA con solo
 * {@code USUARIOS_USUARIO_VER} ({@link IdentidadSaliente}). Si usuarios no
 * responde, falla y el evento se reintenta: una lista vacía dejaría la alerta
 * sin destinatarios (criterio 10).
 */
@Component
public class DirectorioDeUsuariosHttp implements DirectorioDeUsuarios {

    private static final Logger log = LoggerFactory.getLogger(DirectorioDeUsuariosHttp.class);
    private static final ParameterizedTypeReference<List<String>> IDS =
            new ParameterizedTypeReference<>() {};

    private final RestClient http;

    public DirectorioDeUsuariosHttp(RestClient.Builder builder,
            @Value("${regenta.usuarios.url:http://localhost:8081}") String urlDeUsuarios) {
        this.http = builder.baseUrl(urlDeUsuarios).build();
    }

    @Override
    public List<UUID> usuariosConRol(UUID negocioId, String rol) {
        List<String> ids;
        try {
            ids = http.get()
                    .uri("/api/usuarios/usuarios/con-rol?rol={rol}", rol)
                    .headers(c -> IdentidadSaliente.aplicar(c, negocioId, "USUARIOS",
                            "USUARIOS_USUARIO_VER"))
                    .retrieve()
                    .body(IDS);
        } catch (RuntimeException fallo) {
            log.warn("No se pudo consultar servicio-usuarios: {}", fallo.getMessage());
            throw new ServicioNoDisponibleException(
                    "No se pudieron consultar los usuarios con el rol " + rol);
        }
        if (ids == null) {
            throw new ServicioNoDisponibleException("Los usuarios del rol llegaron vacíos");
        }
        return ids.stream().map(UUID::fromString).toList();
    }
}
