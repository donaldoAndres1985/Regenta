package com.regenta.auditoria;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * PostgreSQL de verdad, con las migraciones de verdad y conectado con un
 * usuario que NO es superusuario (mismo patrón que el resto de los servicios).
 */
@SpringBootTest
public abstract class BaseDeAuditoria {

    protected static final String ROL = "reg_auditoria";
    protected static final String CLAVE = "clave_de_prueba";

    static final PostgreSQLContainer<?> PG =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"));

    static {
        PG.start();
        comoSuperusuario("CREATE ROLE " + ROL + " LOGIN PASSWORD '" + CLAVE + "'");
        comoSuperusuario("GRANT ALL ON DATABASE " + PG.getDatabaseName() + " TO " + ROL);
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", PG::getJdbcUrl);
        registro.add("spring.datasource.username", () -> ROL);
        registro.add("spring.datasource.password", () -> CLAVE);
        registro.add("regenta.eventos.publicador-activo", () -> false);
        registro.add("spring.rabbitmq.listener.simple.auto-startup", () -> false);
    }

    protected static <T> T enContexto(UUID negocio, UUID usuario, Set<String> permisos,
            Supplier<T> tarea) {
        AtomicReference<T> resultado = new AtomicReference<>();
        com.regenta.comun.negocio.ContextoDeNegocio.en(
                new com.regenta.comun.negocio.DatosDelNegocio(negocio, usuario, "EMPRESARIAL",
                        "VENTA_DIRECTA", Set.of("ADMINISTRADOR"), Set.of("AUDITORIA"), permisos, Set.of()),
                () -> resultado.set(tarea.get()));
        return resultado.get();
    }

    protected static void enContexto(UUID negocio, UUID usuario, Set<String> permisos, Runnable tarea) {
        enContexto(negocio, usuario, permisos, () -> {
            tarea.run();
            return null;
        });
    }

    protected static void comoSuperusuario(String sentencia) {
        try (Connection conexion = DriverManager.getConnection(
                PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
                Statement declaracion = conexion.createStatement()) {
            declaracion.execute(sentencia);
        } catch (SQLException fallo) {
            throw new IllegalStateException("Falló la sentencia de prueba: " + sentencia, fallo);
        }
    }

    protected static List<String> consultar(String sql) {
        List<String> filas = new ArrayList<>();
        try (Connection conexion = DriverManager.getConnection(
                PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
                Statement declaracion = conexion.createStatement()) {
            declaracion.execute("SET search_path TO auditoria, public");
            try (ResultSet resultado = declaracion.executeQuery(sql)) {
                while (resultado.next()) {
                    filas.add(resultado.getString(1));
                }
            }
        } catch (SQLException fallo) {
            throw new IllegalStateException("Falló la consulta de prueba: " + sql, fallo);
        }
        return filas;
    }

    protected static long contar(String sql) {
        return Long.parseLong(consultar(sql).get(0));
    }

    protected static void ejecutarComoElServicio(UUID negocio, String sql) {
        try (Connection conexion = DriverManager.getConnection(PG.getJdbcUrl(), ROL, CLAVE);
                Statement declaracion = conexion.createStatement()) {
            declaracion.execute("SET search_path TO auditoria, public");
            if (negocio != null) {
                declaracion.execute("SELECT set_config('app.negocio_id', '" + negocio + "', false)");
            }
            declaracion.execute(sql);
        } catch (SQLException fallo) {
            throw new IllegalStateException("PostgreSQL rechazó: " + sql, fallo);
        }
    }

    protected static List<String> comoElServicio(UUID negocio, String sql) {
        List<String> filas = new ArrayList<>();
        try (Connection conexion = DriverManager.getConnection(PG.getJdbcUrl(), ROL, CLAVE);
                Statement declaracion = conexion.createStatement()) {
            declaracion.execute("SET search_path TO auditoria, public");
            if (negocio != null) {
                declaracion.execute("SELECT set_config('app.negocio_id', '" + negocio + "', false)");
            }
            try (ResultSet resultado = declaracion.executeQuery(sql)) {
                while (resultado.next()) {
                    filas.add(resultado.getString(1));
                }
            }
        } catch (SQLException fallo) {
            throw new IllegalStateException("Falló la consulta de prueba: " + sql, fallo);
        }
        return filas;
    }
}
