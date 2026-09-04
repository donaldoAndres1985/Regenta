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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * HU-006 . Row-Level Security en todas las tablas de negocio.
 *
 * <p>Van juntas porque prueban lo mismo a tres alturas: que cada servicio tenga su
 * propia base, que su esquema se cree solo con migraciones, y que dentro de esa base
 * un negocio no pueda ver los datos de otro. Contra PostgreSQL real, nunca contra H2:
 * aqui se prueban permisos entre bases, extensiones, RLS y constraints que una base
 * en memoria no tiene.
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

    private static final String NEGOCIO_A = "11111111-1111-1111-1111-111111111111";
    private static final String NEGOCIO_B = "22222222-2222-2222-2222-222222222222";

    private static int salidaPrimeraEjecucion;
    private static int salidaSegundaEjecucion;
    private static final Map<String, MigrateResult> MIGRACIONES = new LinkedHashMap<>();

    @BeforeAll
    static void crearLasBasesYMigrar() throws Exception {
        salidaPrimeraEjecucion = ejecutarInit();
        salidaSegundaEjecucion = ejecutarInit();   // criterio 3 de HU-004: idempotencia
        assertThat(salidaPrimeraEjecucion).isZero();

        // Se migra una sola vez, aqui, para que cada test parta del mismo estado y
        // no dependa del orden en que JUnit decida ejecutarlos.
        for (String s : Repo.SERVICIOS) {
            Path carpeta = Repo.migracionInicial(s).getParent();
            MIGRACIONES.put(s, flyway(s, Repo.esquema(s), "filesystem:" + carpeta.toAbsolutePath()).migrate());
        }
        sembrarDosNegocios();
    }

    /** Dos negocios con una fila cada uno: sin eso, un test de aislamiento no prueba nada. */
    private static void sembrarDosNegocios() throws SQLException {
        try (Connection c = conexion("inventario"); Statement s = c.createStatement()) {
            fijarNegocio(s, NEGOCIO_A);
            s.executeUpdate("INSERT INTO inventario.marcas (id, negocio_id, nombre, activa) VALUES "
                    + "('aaaaaaaa-0000-0000-0000-00000000000a','" + NEGOCIO_A + "','Marca de A', true)");
            c.commit();
            fijarNegocio(s, NEGOCIO_B);
            s.executeUpdate("INSERT INTO inventario.marcas (id, negocio_id, nombre, activa) VALUES "
                    + "('bbbbbbbb-0000-0000-0000-00000000000b','" + NEGOCIO_B + "','Marca de B', true)");
            c.commit();
        }
    }

    private static Connection conexion(String servicio) throws SQLException {
        Connection c = DriverManager.getConnection(url("regenta_" + servicio), "reg_" + servicio, CLAVE);
        c.setAutoCommit(false);
        return c;
    }

    /**
     * SET LOCAL, no SET. Dura solo esta transaccion. Con un SET normal el negocio se
     * queda pegado a la conexion, y HikariCP la reutiliza para la peticion de otro.
     */
    private static void fijarNegocio(Statement s, String negocio) throws SQLException {
        s.execute("SET LOCAL app.negocio_id = '" + negocio + "'");
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
            assertThat(Repo.migracionInicial(s)).as("falta la V1 de %s", s).exists();

            MigrateResult r = MIGRACIONES.get(s);

            assertThat(r.success).as("la migracion de %s no aplico", s).isTrue();
            assertThat(r.migrationsExecuted).isGreaterThanOrEqualTo(2);

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

    // ------------------------------------------------------------------ HU-006

    /** Una tabla representativa por servicio, para no confiar solo en el conteo. */
    private static final Map<String, String> TABLA_TESTIGO = Map.ofEntries(
            Map.entry("usuarios", "usuarios"), Map.entry("clientes", "clientes"),
            Map.entry("inventario", "productos"), Map.entry("ventas", "ventas"),
            Map.entry("compras", "ordenes_compra"), Map.entry("recursos", "recursos"),
            Map.entry("reservas", "reservas"), Map.entry("menu", "items_menu"),
            Map.entry("mesas", "mesas"), Map.entry("comandas", "comandas"),
            Map.entry("facturacion", "facturas"), Map.entry("caja", "sesiones_caja"),
            Map.entry("alertas", "alertas"), Map.entry("reportes", "hechos_venta"),
            Map.entry("auditoria", "eventos_auditoria"));

    @Test
    @DisplayName("HU-006 criterio 1 . toda tabla con RLS tiene FORCE y la politica tenant_isolation")
    void rlsActivaYForzada() throws Exception {
        int total = 0;
        for (String s : Repo.SERVICIOS) {
            String esquema = Repo.esquema(s);

            // Sin FORCE, el dueno de la tabla —que es el usuario del servicio— se
            // salta la politica y todo esto no sirve de nada.
            String malas = consultar("regenta_" + s, "reg_" + s, CLAVE,
                    "select count(*) from pg_class c join pg_namespace ns on ns.oid = c.relnamespace "
                    + "where ns.nspname = '" + esquema + "' and c.relkind in ('r','p') and c.relrowsecurity "
                    + "and (not c.relforcerowsecurity or not exists ("
                    + "  select 1 from pg_policy p where p.polrelid = c.oid and p.polname = 'tenant_isolation'))")
                    .get(0);
            assertThat(malas).as("%s tiene tablas con RLS sin FORCE o sin politica", s).isEqualTo("0");

            String conRls = consultar("regenta_" + s, "reg_" + s, CLAVE,
                    "select count(*) from pg_class c join pg_namespace ns on ns.oid = c.relnamespace "
                    + "where ns.nspname = '" + esquema + "' and c.relkind in ('r','p') and c.relrowsecurity").get(0);
            assertThat(Integer.parseInt(conRls)).as("%s no tiene ninguna tabla con RLS", s).isPositive();
            total += Integer.parseInt(conRls);

            String testigo = TABLA_TESTIGO.get(s);
            String protegida = consultar("regenta_" + s, "reg_" + s, CLAVE,
                    "select count(*) from pg_class c join pg_namespace ns on ns.oid = c.relnamespace "
                    + "where ns.nspname = '" + esquema + "' and c.relname = '" + testigo + "' "
                    + "and c.relrowsecurity and c.relforcerowsecurity").get(0);
            assertThat(protegida).as("%s.%s quedo sin RLS", esquema, testigo).isEqualTo("1");
        }
        assertThat(total).isGreaterThanOrEqualTo(140);
    }

    @Test
    @DisplayName("HU-006 criterio 2 . sin negocio fijado, la consulta devuelve cero filas")
    void sinNegocioNoSeVeNada() throws Exception {
        try (Connection c = conexion("inventario"); Statement s = c.createStatement()) {
            // Sin SET LOCAL, app_negocio_actual() es NULL y toda comparacion da NULL.
            // Falla cerrado, que es como tiene que fallar.
            try (ResultSet rs = s.executeQuery("select count(*) from inventario.marcas")) {
                rs.next();
                assertThat(rs.getInt(1)).isZero();
            }
            c.commit();
        }
    }

    @Test
    @DisplayName("HU-006 criterio 3 . con el negocio A fijado, la fila de B no aparece ni por id")
    void unNegocioNoVeLoDelOtro() throws Exception {
        try (Connection c = conexion("inventario"); Statement s = c.createStatement()) {
            fijarNegocio(s, NEGOCIO_A);
            try (ResultSet rs = s.executeQuery(
                    "select count(*) from inventario.marcas where id = 'bbbbbbbb-0000-0000-0000-00000000000b'")) {
                rs.next();
                assertThat(rs.getInt(1)).as("A vio una fila de B buscandola por id").isZero();
            }
            try (ResultSet rs = s.executeQuery("select nombre from inventario.marcas")) {
                rs.next();
                assertThat(rs.getString(1)).isEqualTo("Marca de A");
                assertThat(rs.next()).isFalse();
            }
            c.commit();
        }
    }

    @Test
    @DisplayName("HU-006 criterio 4 . la misma conexion, dos negocios, cada uno ve lo suyo")
    void mismaConexionDosNegocios() throws Exception {
        // Esta es la prueba que importa con un pool de conexiones: la conexion se
        // reutiliza y lo unico que cambia es la transaccion.
        try (Connection c = conexion("inventario"); Statement s = c.createStatement()) {
            fijarNegocio(s, NEGOCIO_A);
            assertThat(unicoNombre(s)).isEqualTo("Marca de A");
            c.commit();

            fijarNegocio(s, NEGOCIO_B);
            assertThat(unicoNombre(s)).isEqualTo("Marca de B");
            c.commit();

            // Y al soltar el SET LOCAL con el commit, no queda nada pegado.
            try (ResultSet rs = s.executeQuery("select count(*) from inventario.marcas")) {
                rs.next();
                assertThat(rs.getInt(1)).as("el negocio quedo pegado a la conexion").isZero();
            }
            c.commit();
        }
    }

    private static String unicoNombre(Statement s) throws SQLException {
        try (ResultSet rs = s.executeQuery("select nombre from inventario.marcas")) {
            rs.next();
            String n = rs.getString(1);
            assertThat(rs.next()).as("se vio mas de una fila").isFalse();
            return n;
        }
    }

    @Test
    @DisplayName("HU-006 . un negocio tampoco puede escribir una fila de otro")
    void noSePuedeEscribirEnOtroNegocio() throws Exception {
        try (Connection c = conexion("inventario"); Statement s = c.createStatement()) {
            fijarNegocio(s, NEGOCIO_A);
            assertThatThrownBy(() -> s.executeUpdate(
                    "INSERT INTO inventario.marcas (id, negocio_id, nombre, activa) VALUES "
                    + "('cccccccc-0000-0000-0000-00000000000c','" + NEGOCIO_B + "','Colada', true)"))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("row-level security");
            c.rollback();
        }
    }
}
