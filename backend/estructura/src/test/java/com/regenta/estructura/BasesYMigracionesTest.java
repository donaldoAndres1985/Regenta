package com.regenta.estructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * HU-004 . Una base por microservicio.
 * HU-005 . Migraciones versionadas con Flyway por servicio.
 *
 * <p>Van juntos porque prueban lo mismo desde dos alturas: que cada servicio tenga
 * su propia base y que su esquema se cree solo con migraciones. Contra PostgreSQL
 * real, nunca contra H2: aqui se prueban permisos entre bases, extensiones y
 * constraints que una base en memoria no tiene.
 */
@Testcontainers
class BasesYMigracionesTest {

    private static final String CLAVE = "clave_de_prueba";
    private static final String ADMIN = "regenta_admin";
    private static final String CLAVE_ADMIN = "admin_de_prueba";

    @Container
    private static final PostgreSQLContainer<?> PG =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"))
                    .withUsername(ADMIN)
                    .withPassword(CLAVE_ADMIN)
                    .withDatabaseName("postgres")
                    .withCopyFileToContainer(
                            MountableFile.forHostPath(
                                    Repo.backend().resolve("docker/postgres/init-databases.sql")),
                            "/sql/init-databases.sql");

    private static int salidaPrimeraEjecucion;
    private static int salidaSegundaEjecucion;

    @BeforeAll
    static void crearLasBases() throws Exception {
        salidaPrimeraEjecucion = ejecutarInit();
        salidaSegundaEjecucion = ejecutarInit();   // criterio 3 de HU-004: idempotencia
        assertThat(salidaPrimeraEjecucion).isZero();
    }

    private static int ejecutarInit() throws Exception {
        var r = PG.execInContainer("psql", "-v", "ON_ERROR_STOP=1", "-v", "clave=" + CLAVE,
                "-U", ADMIN, "-d", "postgres", "-f", "/sql/init-databases.sql");
        if (r.getExitCode() != 0) {
            System.out.println(r.getStderr());
        }
        return r.getExitCode();
    }

    private static String url(String base) {
        return "jdbc:postgresql://" + PG.getHost() + ":" + PG.getFirstMappedPort() + "/" + base;
    }

    private static List<String> consultar(String base, String usuario, String clave, String sql) throws SQLException {
        try (Connection c = DriverManager.getConnection(url(base), usuario, clave);
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            List<String> filas = new ArrayList<>();
            while (rs.next()) {
                filas.add(rs.getString(1));
            }
            return filas;
        }
    }

    // ------------------------------------------------------------------ HU-004

    @Test
    @DisplayName("HU-004 criterio 1 . existe una base y un usuario por servicio")
    void unaBasePorServicio() throws Exception {
        List<String> bases = consultar("postgres", ADMIN, CLAVE_ADMIN,
                "select datname from pg_database where datname like 'regenta_%'");
        List<String> roles = consultar("postgres", ADMIN, CLAVE_ADMIN,
                "select rolname from pg_roles where rolname like 'reg_%'");

        for (String s : Repo.SERVICIOS) {
            assertThat(bases).contains("regenta_" + s);
            assertThat(roles).contains("reg_" + s);
        }
        assertThat(bases).hasSize(Repo.SERVICIOS.size());
    }

    @Test
    @DisplayName("HU-004 criterio 2 . un servicio no puede entrar a la base de otro")
    void sinAccesoCruzado() {
        // Ventas contra la base de Usuarios: PostgreSQL lo corta en la conexion,
        // antes de cualquier consulta. Que no dependa de acordarse del WHERE.
        assertThatThrownBy(() -> DriverManager.getConnection(url("regenta_usuarios"), "reg_ventas", CLAVE))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("permission denied");

        assertThatThrownBy(() -> DriverManager.getConnection(url("regenta_ventas"), "reg_inventario", CLAVE))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("permission denied");
    }

    @Test
    @DisplayName("HU-004 criterio 3 . el script de inicializacion es idempotente")
    void inicializacionIdempotente() {
        assertThat(salidaSegundaEjecucion).as("la segunda ejecucion del init fallo").isZero();
    }

    @Test
    @DisplayName("HU-004 criterio 4 . cada application.yml apunta solo a su base")
    void cadaServicioASuBase() {
        for (String s : Repo.SERVICIOS) {
            String yml = Repo.leer(Repo.moduloServicio(s).resolve("src/main/resources/application.yml"));
            assertThat(yml).contains("regenta_" + s);
            assertThat(yml).contains("reg_" + s);
            for (String otro : Repo.SERVICIOS) {
                if (!otro.equals(s)) {
                    assertThat(yml).as("%s menciona la base de %s", s, otro)
                            .doesNotContain("regenta_" + otro);
                }
            }
        }
    }

    // ------------------------------------------------------------------ HU-005

    private static Flyway flyway(String servicio, String esquema, String localizacion) {
        return Flyway.configure()
                .dataSource(url("regenta_" + servicio), "reg_" + servicio, CLAVE)
                .schemas(esquema)
                .defaultSchema(esquema)
                .createSchemas(true)
                .locations(localizacion)
                .load();
    }

    @Test
    @DisplayName("HU-005 criterios 1, 2 y 4 . cada servicio migra su propio esquema desde cero")
    void migracionesPorServicio() throws Exception {
        for (String s : Repo.SERVICIOS) {
            String esquema = Repo.esquema(s);
            Path carpeta = Repo.migracionInicial(s).getParent();
            assertThat(Repo.migracionInicial(s)).as("falta la V1 de %s", s).exists();

            MigrateResult r = flyway(s, esquema, "filesystem:" + carpeta.toAbsolutePath()).migrate();

            assertThat(r.success).as("la migracion de %s no aplico", s).isTrue();
            assertThat(r.migrationsExecuted).isGreaterThanOrEqualTo(1);

            // criterio 4: queda constancia de la version aplicada
            List<String> versiones = consultar("regenta_" + s, "reg_" + s, CLAVE,
                    "select version from " + esquema + ".flyway_schema_history where success");
            assertThat(versiones).contains("1");

            // criterio 2: solo sus tablas, en su esquema
            List<String> tablas = consultar("regenta_" + s, "reg_" + s, CLAVE,
                    "select table_name from information_schema.tables where table_schema = '" + esquema + "'");
            assertThat(tablas).as("el esquema de %s quedo vacio", s).isNotEmpty();
            assertThat(tablas).contains("outbox_eventos", "inbox_eventos");
        }
    }

    @Test
    @DisplayName("HU-005 criterio 3 . modificar una migracion aplicada falla por checksum")
    void checksumProtegeLasMigracionesAplicadas() throws Exception {
        // Se trabaja sobre una copia y un esquema aparte para no ensuciar el resto.
        Path copia = Files.createTempDirectory("regenta-checksum");
        Path archivo = copia.resolve("V1__esquema_inicial.sql");
        Files.writeString(archivo, "CREATE TABLE prueba_checksum (id int primary key);\n",
                StandardCharsets.UTF_8);

        Flyway f = flyway("caja", "prueba_checksum", "filesystem:" + copia.toAbsolutePath());
        assertThat(f.migrate().success).isTrue();

        // Alguien edita una migracion ya aplicada.
        Files.writeString(archivo, "CREATE TABLE prueba_checksum (id int primary key, extra text);\n",
                StandardCharsets.UTF_8);

        Flyway f2 = flyway("caja", "prueba_checksum", "filesystem:" + copia.toAbsolutePath());
        assertThatThrownBy(f2::validate).isInstanceOf(FlywayValidateException.class);
    }

    @Test
    @DisplayName("HU-005 terminado cuando . ddl-auto es validate en todos los servicios")
    void hibernateNuncaTocaElEsquema() {
        for (String s : Repo.SERVICIOS) {
            String yml = Repo.leer(Repo.moduloServicio(s).resolve("src/main/resources/application.yml"));
            assertThat(yml).contains("ddl-auto: validate");
            assertThat(yml).doesNotContain("ddl-auto: update");
            assertThat(yml).doesNotContain("ddl-auto: create");
        }
    }
}
