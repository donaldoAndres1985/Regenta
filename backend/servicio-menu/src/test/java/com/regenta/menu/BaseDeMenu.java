package com.regenta.menu;

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
 * la Row-Level Security sin avisar: los tests de aislamiento pasarian solos sin
 * probar nada. Aqui se crea {@code reg_menu}, el rol con el que se conecta
 * el servicio en produccion, y las tablas quedan a su nombre. Con FORCE, la
 * politica aplica tambien a el.
 *
 * <p>Todo test de repositorio corre con dos negocios cargados: si mañana
 * desaparece el filtro por {@code negocio_id}, un test con datos de un solo
 * negocio seguiria verde.
 */
@SpringBootTest
public abstract class BaseDeMenu {

    protected static final String ROL = "reg_menu";
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
     * Lo que hace el gateway en produccion: deja el negocio, el usuario y sus
     * permisos en el contexto antes de que el caso de uso corra.
     */
    protected static <T> T enContexto(UUID negocio, UUID usuario, Set<String> permisos,
            Supplier<T> tarea) {
        AtomicReference<T> resultado = new AtomicReference<>();
        com.regenta.comun.negocio.ContextoDeNegocio.en(
                new com.regenta.comun.negocio.DatosDelNegocio(negocio, usuario, "BASICO",
                        "COMANDA", Set.of("ADMINISTRADOR"), Set.of("MENU"), permisos,
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

    /** Conexion sin RLS, para comprobar lo que quedo escrito de verdad. */
    protected static void comoSuperusuario(String sentencia) {
        try (Connection conexion = DriverManager.getConnection(
                PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
                Statement declaracion = conexion.createStatement()) {
            declaracion.execute(sentencia);
        } catch (SQLException fallo) {
            throw new IllegalStateException("Fallo la sentencia de prueba: " + sentencia, fallo);
        }
    }

    /** Una consulta de una sola columna, leida como superusuario (sin RLS). */
    protected static List<String> consultar(String sql) {
        List<String> filas = new ArrayList<>();
        try (Connection conexion = DriverManager.getConnection(
                PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
                Statement declaracion = conexion.createStatement()) {
            declaracion.execute("SET search_path TO menu, public");
            try (ResultSet resultado = declaracion.executeQuery(sql)) {
                while (resultado.next()) {
                    filas.add(resultado.getString(1));
                }
            }
        } catch (SQLException fallo) {
            throw new IllegalStateException("Fallo la consulta de prueba: " + sql, fallo);
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
            declaracion.execute("SET search_path TO menu, public");
            if (negocio != null) {
                declaracion.execute("SELECT set_config('app.negocio_id', '" + negocio + "', false)");
            }
            declaracion.execute(sql);
        } catch (SQLException fallo) {
            throw new IllegalStateException("PostgreSQL rechazo: " + sql, fallo);
        }
    }

    /**
     * Una consulta con el rol del servicio, fijando (o no) el negocio: asi se
     * comprueba que la RLS corta de verdad.
     */
    protected static List<String> comoElServicio(UUID negocio, String sql) {
        List<String> filas = new ArrayList<>();
        try (Connection conexion = DriverManager.getConnection(PG.getJdbcUrl(), ROL, CLAVE);
                Statement declaracion = conexion.createStatement()) {
            declaracion.execute("SET search_path TO menu, public");
            if (negocio != null) {
                declaracion.execute("SELECT set_config('app.negocio_id', '" + negocio + "', false)");
            }
            try (ResultSet resultado = declaracion.executeQuery(sql)) {
                while (resultado.next()) {
                    filas.add(resultado.getString(1));
                }
            }
        } catch (SQLException fallo) {
            throw new IllegalStateException("Fallo la consulta de prueba: " + sql, fallo);
        }
        return filas;
    }

    /**
     * El plan de una consulta, leido con el rol del servicio y el {@code seqscan}
     * apagado. Sirve para comprobar que la busqueda por nombre parcial se apoya
     * en un indice acotado al negocio y no barre la tabla entera (HU-021
     * criterio 3): si aun asi apareciera un {@code Seq Scan}, es que no hay
     * indice util.
     */
    protected static String planDe(UUID negocio, String sql) {
        StringBuilder plan = new StringBuilder();
        try (Connection conexion = DriverManager.getConnection(PG.getJdbcUrl(), ROL, CLAVE);
                Statement declaracion = conexion.createStatement()) {
            declaracion.execute("SET search_path TO menu, public");
            declaracion.execute("SET enable_seqscan = off");
            if (negocio != null) {
                declaracion.execute("SELECT set_config('app.negocio_id', '" + negocio + "', false)");
            }
            try (ResultSet resultado = declaracion.executeQuery("EXPLAIN " + sql)) {
                while (resultado.next()) {
                    plan.append(resultado.getString(1)).append('\n');
                }
            }
        } catch (SQLException fallo) {
            throw new IllegalStateException("Fallo el EXPLAIN de prueba: " + sql, fallo);
        }
        return plan.toString();
    }
}
