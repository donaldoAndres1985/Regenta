package com.regenta.comun.openapi;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * El contrato OpenAPI que publica cada servicio. HU-010.
 *
 * <p>Vive en la libreria comun para que los quince servicios digan lo mismo sin
 * copiar y pegar: el mismo esquema de autenticacion y los mismos codigos de
 * error. Un cliente generado contra un servicio sabe leer a todos.
 *
 * <p>Los cuatro codigos comunes se agregan a TODA operacion, porque los cuatro
 * pueden pasar en cualquier endpoint sin que el servicio haga nada: el 401 y el
 * 402 los pone el gateway antes de llegar, y el 403 y el 422 salen del filtro de
 * permisos y de la validacion. Si el generador del cliente Dart no los ve, no
 * genera con que atraparlos.
 */
@AutoConfiguration
@ConditionalOnClass({ OpenAPI.class, OperationCustomizer.class })
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class OpenApiAutoConfiguracion {

    /** Nombre del esquema de seguridad en el contrato. */
    public static final String ESQUEMA_BEARER = "bearerJwt";

    /** Codigos que puede devolver cualquier endpoint, y por que. */
    static final Map<String, String> RESPUESTAS_COMUNES = respuestasComunes();

    private static Map<String, String> respuestasComunes() {
        Map<String, String> respuestas = new LinkedHashMap<>();
        respuestas.put("401", "Token ausente, expirado, con firma invalida o de otro emisor");
        respuestas.put("402", "El negocio esta SUSPENDIDO o CANCELADO");
        respuestas.put("403", "Autenticado, pero sin permiso para esta operacion"
                + " o con el modulo fuera de su plan");
        respuestas.put("422", "La peticion esta bien formada pero rompe una regla de negocio");
        return respuestas;
    }

    @Bean
    @ConditionalOnMissingBean
    public OpenAPI contratoDeRegenta(Environment entorno) {
        String servicio = entorno.getProperty("spring.application.name", "servicio");
        String version = entorno.getProperty("regenta.api.version", "0.1.0");

        Components componentes = new Components()
                .addSecuritySchemes(ESQUEMA_BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT emitido por servicio-usuarios y validado en el gateway."
                                + " Lleva negocio_id, plan, patron, roles y sucursales."));
        RESPUESTAS_COMUNES.forEach((codigo, descripcion) ->
                componentes.addResponses(codigo, new ApiResponse().description(descripcion)));

        return new OpenAPI()
                .info(new Info()
                        .title("Regenta - " + servicio)
                        .version(version)
                        .description("API de " + servicio + ". Todas las rutas exigen el JWT"
                                + " que emite servicio-usuarios; el negocio sale del token,"
                                + " nunca de un parametro."))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_BEARER))
                .components(componentes);
    }

    @Bean
    public OperationCustomizer respuestasComunesDeRegenta() {
        return (operacion, metodo) -> {
            ApiResponses respuestas = operacion.getResponses();
            if (respuestas == null) {
                respuestas = new ApiResponses();
                operacion.setResponses(respuestas);
            }
            for (String codigo : RESPUESTAS_COMUNES.keySet()) {
                if (!respuestas.containsKey(codigo)) {
                    respuestas.addApiResponse(codigo,
                            new ApiResponse().$ref("#/components/responses/" + codigo));
                }
            }
            return operacion;
        };
    }
}
