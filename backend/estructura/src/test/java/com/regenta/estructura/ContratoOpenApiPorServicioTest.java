package com.regenta.estructura;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * HU-010 . Cada servicio publica su contrato OpenAPI.
 *
 * <p>Lo que se comprueba aqui es que ningun servicio se quede fuera y que la
 * dependencia entre a un solo sitio. Que el contrato diga lo correcto se prueba
 * en la libreria comun, con las clases de verdad; que se sirva de verdad, con el
 * primer endpoint real.
 */
class ContratoOpenApiPorServicioTest {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    @Test
    @DisplayName("criterio 1 . los 15 servicios exponen /v3/api-docs y /swagger-ui")
    void todosExponenElContratoYLaInterfaz() throws Exception {
        for (String servicio : Repo.SERVICIOS) {
            Path yml = Repo.moduloServicio(servicio).resolve("src/main/resources/application.yml");
            JsonNode configuracion = YAML.readTree(yml.toFile());

            assertThat(configuracion.path("springdoc").path("api-docs").path("path").asText())
                    .as("contrato de servicio-%s", servicio)
                    .isEqualTo("/v3/api-docs");
            assertThat(configuracion.path("springdoc").path("swagger-ui").path("path").asText())
                    .as("swagger-ui de servicio-%s", servicio)
                    .isEqualTo("/swagger-ui");
        }
    }

    @Test
    @DisplayName("la dependencia entra una sola vez, por comun, con la version en el pom padre")
    void laDependenciaEntraPorComun() {
        String padre = Repo.leer(Repo.backend().resolve("pom.xml"));
        String comun = Repo.leer(Repo.backend().resolve("comun/pom.xml"));

        assertThat(padre).contains("<springdoc.version>");
        assertThat(comun).contains("springdoc-openapi-starter-webmvc-ui");

        for (String servicio : Repo.SERVICIOS) {
            String pom = Repo.leer(Repo.moduloServicio(servicio).resolve("pom.xml"));
            assertThat(pom)
                    .as("servicio-%s no debe declarar springdoc: llega por comun", servicio)
                    .doesNotContain("springdoc");
            assertThat(pom)
                    .as("servicio-%s depende de comun", servicio)
                    .contains("<artifactId>comun</artifactId>");
        }
    }
}
