package com.regenta.shared.bbdd;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.regenta.PostgresDePrueba;

/**
 * HU-125 · La aplicación entera arranca como en Railway: migraciones, JPA en {@code validate}, la
 * verificación del entorno (aquí, desarrollo) y el actuator. El healthcheck del despliegue
 * ({@code /actuator/health/readiness}) responde, {@code /actuator/health} muestra cada componente (C5) y
 * {@code /actuator/info} dice qué versión está atendiendo, que es lo que espera el pipeline.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ArranqueCompletoEnLaBaseTest extends PostgresDePrueba {

    @Autowired
    private TestRestTemplate http;

    @Test
    void elHealthcheckDelDespliegueRespondeArriba() {
        ResponseEntity<Map> r = http.getForEntity("/actuator/health/readiness", Map.class);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).containsEntry("status", "UP");
    }

    @Test
    @SuppressWarnings("unchecked")
    void c5_laSaludMuestraCadaComponenteSinDetalles() {
        ResponseEntity<Map> r = http.getForEntity("/actuator/health", Map.class);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> componentes = (Map<String, Object>) r.getBody().get("components");
        assertThat(componentes).containsKeys("db", "almacen", "dian", "livenessState", "readinessState");
        assertThat((Map<String, Object>) componentes.get("db")).containsEntry("status", "UP")
                .doesNotContainKey("details");
        assertThat((Map<String, Object>) componentes.get("dian")).containsEntry("status", "UNKNOWN");
    }

    @Test
    @SuppressWarnings("unchecked")
    void laVersionQueAtiendeSeVeEnInfo() {
        Map<String, Object> info = http.getForObject("/actuator/info", Map.class);

        assertThat((Map<String, Object>) info.get("app")).containsEntry("version", "dev");
    }
}
