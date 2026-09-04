package com.regenta.comun.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;

/** HU-010. Lo que todo servicio de Regenta promete en su contrato. */
class ContratoOpenApiTest {

    private final OpenApiAutoConfiguracion configuracion = new OpenApiAutoConfiguracion();

    @Test
    @DisplayName("El contrato lleva el nombre del servicio y el esquema bearer")
    void elContratoSePresenta() {
        MockEnvironment entorno = new MockEnvironment()
                .withProperty("spring.application.name", "servicio-ventas");

        OpenAPI contrato = configuracion.contratoDeRegenta(entorno);

        assertThat(contrato.getInfo().getTitle()).contains("servicio-ventas");
        assertThat(contrato.getInfo().getVersion()).isNotBlank();
        assertThat(contrato.getSecurity()).isNotEmpty();

        SecurityScheme esquema = contrato.getComponents().getSecuritySchemes()
                .get(OpenApiAutoConfiguracion.ESQUEMA_BEARER);
        assertThat(esquema.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(esquema.getScheme()).isEqualTo("bearer");
        assertThat(esquema.getBearerFormat()).isEqualTo("JWT");
    }

    @Test
    @DisplayName("Criterio 2: el contrato declara 401, 402, 403 y 422 como respuestas reutilizables")
    void elContratoDeclaraLosCodigosComunes() {
        OpenAPI contrato = configuracion.contratoDeRegenta(new MockEnvironment());

        assertThat(contrato.getComponents().getResponses())
                .containsKeys("401", "402", "403", "422");
        assertThat(contrato.getComponents().getResponses().get("402").getDescription())
                .contains("SUSPENDIDO");
    }

    @Test
    @DisplayName("Criterio 2: toda operacion los declara, sin que el servicio los escriba")
    void todaOperacionHeredaLosCodigosComunes() {
        Operation operacion = new Operation();

        configuracion.respuestasComunesDeRegenta().customize(operacion, null);

        assertThat(operacion.getResponses()).containsKeys("401", "402", "403", "422");
        assertThat(operacion.getResponses().get("402").get$ref())
                .isEqualTo("#/components/responses/402");
    }

    @Test
    @DisplayName("Si el servicio ya documento un codigo, el suyo manda")
    void noPisaLoQueElServicioYaDocumento() {
        Operation operacion = new Operation().responses(new ApiResponses()
                .addApiResponse("403", new ApiResponse().description("El turno de caja no es suyo")));

        configuracion.respuestasComunesDeRegenta().customize(operacion, null);

        assertThat(operacion.getResponses().get("403").getDescription())
                .isEqualTo("El turno de caja no es suyo");
        assertThat(operacion.getResponses()).containsKeys("401", "402", "422");
    }
}
