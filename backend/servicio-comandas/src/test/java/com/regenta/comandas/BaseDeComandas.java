package com.regenta.comandas;

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
 * usuario que NO es superusuario.
 *
 * <p>El rol que crea Testcontainers es superusuario, y un superusuario se salta
 * la Row-Level Security sin avisar: los tests de aislamiento pasarían solos sin
 * probar nada. Aquí se crea {@code reg_comandas}, el rol con el que se conecta el
 * servicio en producción, y las tablas quedan a su nombre. Con FORCE, la
 * política aplica también a él.
 *
 * <p>Todo test de repositorio corre con dos negocios cargados: si mañana
 * desaparece el filtro por {@code negocio_id}, un test con datos de un solo
 * negocio seguiría verde.
 */
@SpringBootTest
public abstract class BaseDeComandas {

    protected static final String ROL = "reg_comandas";
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

    /**
     * Lo que hace el gateway en producción: deja el negocio, el usuario y sus
     * permisos en el contexto antes de que el caso de uso corra.
     */
    protected static <T> T enContexto(UUID negocio, UUID usuario, Set<String> permisos,
            Supplier<T> tarea) {
        AtomicReference<T> resultado = new AtomicReference<>();
        com.regenta.comun.negocio.ContextoDeNegocio.en(
                new com.regenta.comun.negocio.DatosDelNegocio(negocio, usuario, "BASICO",
                        "COMANDA", Set.of("ADMINISTRADOR"), Set.of("COMANDAS"), permisos,
                        Set.of()),
                () -> resultado.set(tarea.get()));
        return resultado.get();
    }

    protected static void enContexto(UUID negocio, UUID usuario, Set<String> permisos,
            Runnable tarea) {
        enContexto(negocio, usuario, permisos, () -> {
            tarea.run();
            return null;
        });
    }

    /** Conexión sin RLS, para comprobar lo que quedó escrito de verdad. */
    protected static void comoSuperusuario(String sentencia) {
        try (Connection conexion = DriverManager.getConnection(
                PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
                Statement declaracion = conexion.createStatement()) {
            declaracion.execute(sentencia);
        } catch (SQLException fallo) {
            throw new IllegalStateException("Falló la sentencia de prueba: " + sentencia, fallo);
        }
    }

    /** Una consulta de una sola columna, leída como superusuario (sin RLS). */
    protected static List<String> consultar(String sql) {
        List<String> filas = new ArrayList<>();
        try (Connection conexion = DriverManager.getConnection(
                PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
                Statement declaracion = conexion.createStatement()) {
            declaracion.execute("SET search_path TO comandas, public");
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

    /**
     * Ejecuta una sentencia con el rol del servicio y el negocio fijado. Lanza
     * {@link IllegalStateException} si PostgreSQL la rechaza: sirve para
     * comprobar triggers y CHECKs.
     */
    protected static void ejecutarComoElServicio(UUID negocio, String sql) {
        try (Connection conexion = DriverManager.getConnection(PG.getJdbcUrl(), ROL, CLAVE);
                Statement declaracion = conexion.createStatement()) {
            declaracion.execute("SET search_path TO comandas, public");
            if (negocio != null) {
                declaracion.execute("SELECT set_config('app.negocio_id', '" + negocio + "', false)");
            }
            declaracion.execute(sql);
        } catch (SQLException fallo) {
            throw new IllegalStateException("PostgreSQL rechazó: " + sql, fallo);
        }
    }

    /**
     * Una consulta con el rol del servicio, fijando (o no) el negocio: así se
     * comprueba que la RLS corta de verdad.
     */
    protected static List<String> comoElServicio(UUID negocio, String sql) {
        List<String> filas = new ArrayList<>();
        try (Connection conexion = DriverManager.getConnection(PG.getJdbcUrl(), ROL, CLAVE);
                Statement declaracion = conexion.createStatement()) {
            declaracion.execute("SET search_path TO comandas, public");
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
