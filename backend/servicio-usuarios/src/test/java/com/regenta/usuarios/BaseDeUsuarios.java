package com.regenta.usuarios;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * PostgreSQL de verdad, con las migraciones de verdad y —esto es lo que
 * importa— conectado con un usuario que NO es superusuario.
 *
 * <p>El usuario que crea Testcontainers es superusuario, y un superusuario se
 * salta la Row-Level Security sin avisar: los tests de aislamiento pasarian
 * solos sin probar nada. Asi que aqui se crea {@code reg_usuarios}, que es el
 * rol con el que se conecta el servicio en produccion, y las tablas quedan a su
 * nombre. Con FORCE, la politica aplica tambien a el.
 */
@SpringBootTest
public abstract class BaseDeUsuarios {

    protected static final String ROL = "reg_usuarios";
    protected static final String CLAVE = "clave_de_prueba";

    /** El mismo que usaria el gateway. Minimo 32 caracteres. */
    protected static final String SECRETO_DE_PRUEBAS =
            "secreto-de-pruebas-de-regenta-con-mas-de-32-caracteres";

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
        registro.add("regenta.operador.clave", () -> "clave-del-operador-de-pruebas");
        registro.add("regenta.jwt.secreto", () -> SECRETO_DE_PRUEBAS);
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
            declaracion.execute("SET search_path TO core_identidad, public");
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
     * Una consulta con el rol del servicio, fijando (o no) el negocio: es asi
     * como se comprueba que la RLS corta de verdad.
     */
    protected static List<String> comoElServicio(UUID negocio, String sql) {
        List<String> filas = new ArrayList<>();
        try (Connection conexion = DriverManager.getConnection(PG.getJdbcUrl(), ROL, CLAVE);
                Statement declaracion = conexion.createStatement()) {
            declaracion.execute("SET search_path TO core_identidad, public");
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
}
