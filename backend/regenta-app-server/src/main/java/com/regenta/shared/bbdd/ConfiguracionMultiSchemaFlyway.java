package com.regenta.shared.bbdd;

import java.util.List;
import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.persistence.EntityManagerFactory;

/**
 * Ejecutor programático de migraciones Flyway para los 15 módulos del monolito.
 *
 * <p>Garantiza que cada módulo ejecute sus migraciones de esquema en orden
 * dentro de su propio schema de PostgreSQL y con su propia tabla aislada
 * {@code flyway_schema_history}, antes de que JPA/Hibernate inicialice el
 * {@link EntityManagerFactory}.
 */
@Configuration
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
    public static BeanPostProcessor flywayMultiSchemaRunner(DataSource dataSource) {
        return new BeanPostProcessor() {
            private boolean ejecutado = false;

            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                if (!ejecutado && bean instanceof EntityManagerFactory) {
                    ejecutarMigraciones(dataSource);
                    ejecutado = true;
                }
                return bean;
            }
        };
    }

    public static void ejecutarMigraciones(DataSource dataSource) {
        log.info("Iniciando ejecución de migraciones Flyway multi-schema...");
        for (ModuloMigracion m : MODULOS) {
            log.info("Ejecutando Flyway para '{}' en schema '{}' desde '{}'...",
                    m.modulo(), m.schema(), m.location());
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .schemas(m.schema())
                    .defaultSchema(m.schema())
                    .locations(m.location())
                    .table("flyway_schema_history")
                    .baselineOnMigrate(true)
                    .load();
            flyway.migrate();
        }
        log.info("Todas las migraciones Flyway multi-schema se completaron exitosamente.");
    }
}
