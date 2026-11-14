package com.regenta;

import java.util.UUID;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import com.regenta.shared.bbdd.ConfiguracionMultiSchemaFlyway;

/**
 * Base de los tests que tocan PostgreSQL: un contenedor 16 real (nunca H2), con las
 * migraciones de los 15 schemas aplicadas y Hibernate en {@code validate}.
 *
 * <p>Se conecta con {@code regenta_app}, que no es superusuario: la RLS se aplica de verdad.
 * Para actuar como un negocio, {@link #comoElNegocio} fija {@code app.negocio_id} en la
 * transaccion del test, igual que {@code AislamientoPorNegocio} en produccion.
 */
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ConfiguracionMultiSchemaFlyway.class)
@EntityScan(basePackages = "com.regenta")
public abstract class PostgresDePrueba {

    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withInitScript("postgres-de-prueba.sql");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void baseDePrueba(DynamicPropertyRegistry propiedades) {
        String url = POSTGRES.getJdbcUrl().replace("/" + POSTGRES.getDatabaseName(), "/regenta_prueba");
        propiedades.add("spring.datasource.url", () -> url);
        propiedades.add("spring.datasource.username", () -> "regenta_app");
        propiedades.add("spring.datasource.password", () -> "regenta_app");
        propiedades.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        propiedades.add("spring.flyway.enabled", () -> "false");
    }

    /** Fija el negocio de la transaccion en curso, como lo hace {@code AislamientoPorNegocio}. */
    protected static void comoElNegocio(JdbcTemplate jdbc, UUID negocioId) {
        jdbc.queryForObject("select set_config('app.negocio_id', ?, true)", String.class,
                negocioId.toString());
    }
}
