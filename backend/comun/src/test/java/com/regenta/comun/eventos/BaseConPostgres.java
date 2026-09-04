package com.regenta.comun.eventos;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * PostgreSQL real y el esquema creado con las MIGRACIONES DE VERDAD.
 *
 * <p>Se apunta Flyway a las migraciones de servicio-ventas en vez de escribir aqui un
 * `create table` de mentira: asi, si el dia que cambie la tabla del Outbox alguien
 * olvida la entidad, este test falla. Con `ddl-auto: validate`, Hibernate ademas
 * compara el mapeo contra la tabla real al arrancar.
 */
@Testcontainers
@SpringBootTest(classes = AplicacionDePrueba.class)
public abstract class BaseConPostgres {

    @Container
    static final PostgreSQLContainer<?> PG =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"));

    static Path migracionesDeVentas() {
        Path comun = Paths.get("").toAbsolutePath();
        Path ruta = comun.resolveSibling("servicio-ventas")
                .resolve("src/main/resources/db/migration");
        if (!Files.exists(ruta)) {
            throw new IllegalStateException("No encuentro las migraciones en " + ruta);
        }
        return ruta;
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
        r.add("spring.flyway.locations", () -> "filesystem:" + migracionesDeVentas());
        r.add("spring.flyway.schemas", () -> "ventas");
        r.add("spring.flyway.default-schema", () -> "ventas");
        r.add("spring.flyway.create-schemas", () -> "true");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        r.add("spring.jpa.properties.hibernate.default_schema", () -> "ventas");
        // El latido queda apagado: cada test publica cuando quiere, no cuando pasa el reloj.
        r.add("regenta.eventos.publicador-activo", () -> "false");
    }
}
