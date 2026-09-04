package com.regenta.estructura;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * HU-003 . Levantar el entorno local con Docker Compose.
 *
 * <p>Estos tests leen el compose; no levantan contenedores. Que `docker compose up`
 * funcione de verdad se comprueba levantandolo, y eso no es trabajo de la suite:
 * lo que la suite evita es que alguien borre un volumen nombrado, cambie la version
 * de PostgreSQL o quite el panel de RabbitMQ sin darse cuenta.
 */
class EntornoLocalTest {

    private static JsonNode compose;
    private static Path backend;

    @BeforeAll
    static void cargar() throws Exception {
        backend = Repo.backend();
        compose = new ObjectMapper(new YAMLFactory()).readTree(backend.resolve("docker-compose.yml").toFile());
    }

    private static JsonNode servicio(String nombre) {
        JsonNode n = compose.path("services").path(nombre);
        assertThat(n.isMissingNode()).as("falta el servicio %s en el compose", nombre).isFalse();
        return n;
    }

    private static List<String> puertos(JsonNode servicio) {
        List<String> out = new ArrayList<>();
        servicio.path("ports").forEach(p -> out.add(p.asText()));
        return out;
    }

    @Test
    @DisplayName("criterio 1 . PostgreSQL 16, RabbitMQ y los 16 modulos estan declarados")
    void todoDeclarado() {
        assertThat(servicio("postgres").path("image").asText()).startsWith("postgres:16");
        assertThat(servicio("rabbitmq").path("image").asText()).contains("rabbitmq");

        assertThat(servicio("gateway").path("build").path("args").path("MODULO").asText())
                .isEqualTo("gateway");
        for (String s : Repo.SERVICIOS) {
            JsonNode svc = servicio("servicio-" + s);
            assertThat(svc.path("build").path("args").path("MODULO").asText()).isEqualTo("servicio-" + s);
            // Cada servicio apunta SOLO a su base.
            assertThat(svc.path("environment").path("DB_URL").asText()).endsWith("/regenta_" + s);
            assertThat(svc.path("environment").path("DB_USER").asText()).isEqualTo("reg_" + s);
        }
    }

    @Test
    @DisplayName("criterio 2 . el panel de RabbitMQ queda publicado en 15672")
    void panelDeRabbit() {
        JsonNode rabbit = servicio("rabbitmq");
        assertThat(rabbit.path("image").asText()).contains("management");
        assertThat(puertos(rabbit)).anyMatch(p -> p.contains("15672"));
        assertThat(puertos(rabbit)).anyMatch(p -> p.contains("5672:"));
    }

    @Test
    @DisplayName("criterio 3 . los datos sobreviven al reinicio en volumenes nombrados")
    void volumenesNombrados() {
        assertThat(compose.path("volumes").has("regenta_postgres_data")).isTrue();
        assertThat(compose.path("volumes").has("regenta_rabbitmq_data")).isTrue();

        assertThat(servicio("postgres").path("volumes").toString())
                .contains("regenta_postgres_data:/var/lib/postgresql/data");
        assertThat(servicio("rabbitmq").path("volumes").toString())
                .contains("regenta_rabbitmq_data:/var/lib/rabbitmq");
    }

    @Test
    @DisplayName("criterio 4 . los servicios esperan a que la infraestructura este sana")
    void esperanInfraestructuraSana() {
        assertThat(servicio("postgres").path("healthcheck").path("test").toString()).contains("pg_isready");
        assertThat(servicio("rabbitmq").path("healthcheck").path("test").toString()).contains("rabbitmq-diagnostics");

        for (String s : Repo.SERVICIOS) {
            JsonNode dep = servicio("servicio-" + s).path("depends_on");
            assertThat(dep.path("postgres").path("condition").asText()).isEqualTo("service_healthy");
            assertThat(dep.path("rabbitmq").path("condition").asText()).isEqualTo("service_healthy");
        }
    }

    @Test
    @DisplayName("terminado cuando . el override es local y las credenciales no se versionan")
    void credencialesFueraDelRepo() {
        String gitignore = Repo.leer(Repo.raiz().resolve(".gitignore"));
        assertThat(gitignore).contains("docker-compose.override.yml");
        assertThat(gitignore).contains(".env");

        Path ejemplo = backend.resolve(".env.example");
        assertThat(ejemplo).exists();
        String env = Repo.leer(ejemplo);
        assertThat(env).contains("POSTGRES_USER", "POSTGRES_PASSWORD",
                "REGENTA_DB_PASSWORD", "RABBIT_USER", "RABBIT_PASSWORD");

        // Ningun secreto literal en el compose: todo sale de variables.
        String texto = Repo.leer(backend.resolve("docker-compose.yml"));
        assertThat(texto).doesNotContain("PASSWORD: cambiar_en_local");
    }
}
