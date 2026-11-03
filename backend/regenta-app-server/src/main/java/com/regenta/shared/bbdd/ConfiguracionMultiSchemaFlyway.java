package com.regenta.shared.bbdd;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ejecutor programático de migraciones Flyway para los 15 módulos del monolito.
 *
 * <p>Garantiza que cada módulo ejecute sus migraciones de esquema en orden
 * dentro de su propio schema de PostgreSQL y con su propia tabla aislada
 * {@code flyway_schema_history}, ANTES de que JPA/Hibernate inicialice el
 * {@code EntityManagerFactory}.
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore(HibernateJpaAutoConfiguration.class)
public class ConfiguracionMultiSchemaFlyway {

    private static final Logger log = LoggerFactory.getLogger(ConfiguracionMultiSchemaFlyway.class);

    public record ModuloMigracion(String modulo, String schema, String location) {}

    public static final List<ModuloMigracion> MODULOS = List.of(
            new ModuloMigracion("usuarios", "core_identidad", "classpath:db/migration/usuarios"),
            new ModuloMigracion("clientes", "crm", "classpath:db/migration/clientes"),
            new ModuloMigracion("recursos", "recursos", "classpath:db/migration/recursos"),
            new ModuloMigracion("inventario", "inventario", "classpath:db/migration/inventario"),
            new ModuloMigracion("compras", "compras", "classpath:db/migration/compras"),
            new ModuloMigracion("menu", "menu", "classpath:db/migration/menu"),
            new ModuloMigracion("mesas", "mesas", "classpath:db/migration/mesas"),
            new ModuloMigracion("reservas", "reservas", "classpath:db/migration/reservas"),
            new ModuloMigracion("comandas", "comandas", "classpath:db/migration/comandas"),
            new ModuloMigracion("ventas", "ventas", "classpath:db/migration/ventas"),
            new ModuloMigracion("caja", "caja", "classpath:db/migration/caja"),
            new ModuloMigracion("facturacion", "facturacion", "classpath:db/migration/facturacion"),
            new ModuloMigracion("alertas", "alertas", "classpath:db/migration/alertas"),
            new ModuloMigracion("reportes", "reportes", "classpath:db/migration/reportes"),
            new ModuloMigracion("auditoria", "auditoria", "classpath:db/migration/auditoria")
    );

    @Bean
    public FlywayMultiSchemaRunner flywayMultiSchemaRunner(DataSource dataSource) {
        return new FlywayMultiSchemaRunner(dataSource);
    }

    @Bean
    public static EntityManagerFactoryDependsOnPostProcessor entityManagerFactoryDependsOnFlyway() {
        return new EntityManagerFactoryDependsOnPostProcessor("flywayMultiSchemaRunner");
    }

    public static class FlywayMultiSchemaRunner implements InitializingBean {

        private final DataSource dataSource;

        public FlywayMultiSchemaRunner(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        @Override
        public void afterPropertiesSet() throws Exception {
            inicializarBaseDeDatos();
            ejecutarMigraciones();
        }

        private void inicializarBaseDeDatos() {
            try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute("""
                    CREATE EXTENSION IF NOT EXISTS "pgcrypto";
                    CREATE EXTENSION IF NOT EXISTS "btree_gist";
                    CREATE EXTENSION IF NOT EXISTS "pg_trgm";
                    CREATE EXTENSION IF NOT EXISTS "unaccent";
                    CREATE TABLE IF NOT EXISTS public.event_publication (
                        id UUID NOT NULL,
                        listener_id VARCHAR(512) NOT NULL,
                        event_type VARCHAR(512) NOT NULL,
                        publication_date TIMESTAMPTZ NOT NULL,
                        serialized_event TEXT NOT NULL,
                        completion_date TIMESTAMPTZ,
                        PRIMARY KEY (id)
                    );
                """);
                log.info("Extensiones PostgreSQL (pgcrypto, btree_gist, pg_trgm, unaccent) y tabla public.event_publication listas.");
            } catch (Exception e) {
                log.warn("Aviso durante inicialización base: {}", e.getMessage());
            }
        }

        public void ejecutarMigraciones() {
            log.info("Iniciando ejecución de migraciones Flyway multi-schema...");
            for (ModuloMigracion m : MODULOS) {
                log.info("Migrando esquema '{}'...", m.schema());
                Flyway flyway = Flyway.configure()
                        .dataSource(dataSource)
                        .schemas(m.schema())
                        .defaultSchema(m.schema())
                        .createSchemas(true)
                        .locations(m.location())
                        .table("flyway_schema_history")
                        .baselineOnMigrate(true)
                        .load();
                flyway.repair();
                MigrateResult result = flyway.migrate();
                log.info("Migrando esquema '{}'... {} migraciones aplicadas", m.schema(), result.migrationsExecuted);
            }
            log.info("Todas las migraciones Flyway multi-schema se completaron exitosamente.");
        }
    }
}
