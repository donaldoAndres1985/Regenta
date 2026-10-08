package com.regenta.integracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.DatosDelNegocio;
import com.regenta.inventario.InventarioApplication;
import com.regenta.inventario.aplicacion.GestionDeBodegas;
import com.regenta.inventario.aplicacion.GestionDeCategorias;
import com.regenta.inventario.aplicacion.GestionDeProductos;
import com.regenta.inventario.aplicacion.LibroMayorDeInventario;
import com.regenta.inventario.aplicacion.SolicitudDeBodega;
import com.regenta.inventario.aplicacion.SolicitudDeCategoria;
import com.regenta.inventario.aplicacion.SolicitudDeMovimiento;
import com.regenta.inventario.aplicacion.SolicitudDeProducto;
import com.regenta.inventario.domain.OrigenMovimiento;
import com.regenta.inventario.domain.TipoMovimiento;
import com.regenta.ventas.VentasApplication;
import com.regenta.ventas.aplicacion.GestionDeVentas;
import com.regenta.ventas.aplicacion.SagaDeConfirmacionDeVenta;
import com.regenta.ventas.aplicacion.SolicitudDeLinea;
import com.regenta.ventas.aplicacion.SolicitudDeVenta;

/**
 * HU-127. La saga de stock con los dos servicios levantados, cada uno con su
 * base, y RabbitMQ de verdad entre ellos: el evento sale por el outbox de uno,
 * pasa por el broker y lo consume el otro. Hasta aquí cada mitad se probaba
 * llamando a sus métodos, con los {@code @RabbitListener} apagados.
 *
 * <p>Los dos servicios corren en la misma JVM, así que sus clases comparten
 * classpath. Cada uno arranca con su configuración explícita (no con el
 * {@code application.yml} del otro) y con {@code ddl-auto=none}: el
 * {@code @EntityScan("com.regenta")} de comun ve las entidades de los dos, y
 * la validación del esquema de cada servicio ya la hacen sus propias pruebas.
 *
 * <p>Nada de {@code Thread.sleep}: se espera con Awaitility a que el estado
 * llegue, con un tope.
 */
class SagaDeStockDePuntaAPuntaTest {

    private static final String CLAVE = "clave_de_prueba";
    private static final Duration TOPE = Duration.ofSeconds(30);
    private static final Set<String> VENDEDOR =
            Set.of("VENTAS_VENTA_CREAR", "VENTAS_VENTA_VER", "VENTAS_VENTA_CONFIRMAR");
    private static final Set<String> BODEGUERO = Set.of("INVENTARIO_CATEGORIA_CREAR",
            "INVENTARIO_PRODUCTO_CREAR", "INVENTARIO_BODEGA_CREAR", "INVENTARIO_RESERVA_GESTIONAR");

    static final PostgreSQLContainer<?> PG =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"));
    static final RabbitMQContainer RABBIT =
            new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management"));

    private static ConfigurableApplicationContext ventas;
    private static ConfigurableApplicationContext inventario;

    private final UUID negocio = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();
    private UUID producto;
    private UUID bodega;

    @BeforeAll
    static void levantarLosDosServicios() {
        PG.start();
        RABBIT.start();
        for (String servicio : List.of("ventas", "inventario")) {
            comoSuperusuario("postgres", "CREATE ROLE reg_" + servicio + " LOGIN PASSWORD '" + CLAVE + "'");
            comoSuperusuario("postgres", "CREATE DATABASE regenta_" + servicio + " OWNER reg_" + servicio);
        }
        ventas = arrancar(VentasApplication.class, "ventas");
        inventario = arrancar(InventarioApplication.class, "inventario");
    }

    @AfterAll
    static void apagar() {
        if (inventario != null) {
            inventario.close();
        }
        if (ventas != null) {
            ventas.close();
        }
    }

    private static ConfigurableApplicationContext arrancar(Class<?> aplicacion, String servicio) {
        Map<String, Object> p = new LinkedHashMap<>();
        // Solo esta configuración: el application.yml del otro servicio también está en el classpath.
        p.put("spring.config.location", "optional:classpath:/no-existe/");
        p.put("spring.application.name", "servicio-" + servicio);
        p.put("server.port", "0");
        p.put("spring.datasource.url", url("regenta_" + servicio));
        p.put("spring.datasource.username", "reg_" + servicio);
        p.put("spring.datasource.password", CLAVE);
        p.put("spring.jpa.open-in-view", "false");
        p.put("spring.jpa.hibernate.ddl-auto", "none");
        p.put("spring.jpa.properties.hibernate.default_schema", servicio);
        p.put("spring.jpa.properties.hibernate.jdbc.time_zone", "UTC");
        p.put("spring.flyway.schemas", servicio);
        p.put("spring.flyway.default-schema", servicio);
        p.put("spring.flyway.create-schemas", "true");
        p.put("spring.flyway.locations", "filesystem:" + migraciones(servicio));
        p.put("spring.rabbitmq.host", RABBIT.getHost());
        p.put("spring.rabbitmq.port", RABBIT.getAmqpPort());
        p.put("spring.rabbitmq.username", RABBIT.getAdminUsername());
        p.put("spring.rabbitmq.password", RABBIT.getAdminPassword());
        p.put("regenta.eventos.intervalo", "PT0.2S");
        return new SpringApplicationBuilder(aplicacion).properties(p).run();
    }

    private static Path migraciones(String servicio) {
        Path ruta = Paths.get("").toAbsolutePath().resolveSibling("servicio-" + servicio)
                .resolve("src/main/resources/db/migration");
        if (!Files.exists(ruta)) {
            throw new IllegalStateException("No encuentro las migraciones en " + ruta);
        }
        return ruta;
    }

    @BeforeEach
    void productoConStockEnInventario() {
        enNegocio(BODEGUERO, () -> {
            UUID categoria = inventario.getBean(GestionDeCategorias.class)
                    .crear(new SolicitudDeCategoria("Herramientas", null, null, null, null, null)).id();
            GestionDeProductos productos = inventario.getBean(GestionDeProductos.class);
            UUID unidad = productos.crearUnidad("UND", "Unidad", false);
            producto = productos.crear(new SolicitudDeProducto("MART-16", null, "Martillo", null,
                    categoria, unidad, null, null, new BigDecimal("32000"), null, null, null,
                    false, false, false, false, null)).id();
            bodega = inventario.getBean(GestionDeBodegas.class)
                    .crear(new SolicitudDeBodega("PPAL", "Principal", null, null)).id();
            inventario.getBean(LibroMayorDeInventario.class).registrar(new SolicitudDeMovimiento(
                    producto, bodega, TipoMovimiento.ENTRADA_COMPRA, new BigDecimal("5"),
                    OrigenMovimiento.CARGA_INICIAL, null, "carga inicial", "carga-" + producto));
            return null;
        });
    }

    /** Una venta en borrador por {@code cantidad} martillos. */
    private UUID ventaPor(String cantidad) {
        GestionDeVentas gestion = ventas.getBean(GestionDeVentas.class);
        return enNegocio(VENDEDOR, () -> {
            UUID id = gestion.crearBorrador(new SolicitudDeVenta(bodega, null, null, "MOSTRADOR")).id();
            gestion.agregarLinea(id, new SolicitudDeLinea(producto, "MART-16", "Martillo", "UND",
                    new BigDecimal(cantidad), new BigDecimal("32000"), BigDecimal.ZERO, "IVA19",
                    new BigDecimal("19"), new BigDecimal("20000")));
            return id;
        });
    }

    private void confirmar(UUID venta) {
        enNegocio(VENDEDOR, () -> {
            ventas.getBean(GestionDeVentas.class).confirmar(venta);
            return null;
        });
    }

    @Test
    @DisplayName("Criterio 1: con stock suficiente se reserva, la venta queda CONFIRMADA y se publica venta_completada")
    void conStockLaVentaSeConfirma() {
        UUID venta = ventaPor("2");

        confirmar(venta);

        await().atMost(TOPE).until(() -> estadoVenta(venta).equals("CONFIRMADA"));
        assertThat(estadoSaga(venta)).isEqualTo("COMPLETADA");
        await().atMost(TOPE).until(() -> publicado("ventas", venta, "venta_completada"));
        // venta_completada vuelve a Inventario y la reserva pasa a salida real.
        await().atMost(TOPE).until(() -> estadosDeReserva(venta).equals(List.of("CONFIRMADA")));
        assertThat(cantidadReservada()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Criterio 2: sin stock, la venta vuelve a BORRADOR con el motivo y la saga queda COMPENSADA")
    void sinStockLaSagaCompensa() {
        UUID venta = ventaPor("9");

        confirmar(venta);

        await().atMost(TOPE).until(() -> estadoVenta(venta).equals("BORRADOR"));
        assertThat(estadoSaga(venta)).isEqualTo("COMPENSADA");
        assertThat(consultar("ventas", "select nota from ventas.ventas where id = '" + venta + "'"))
                .singleElement().asString().contains("faltan").contains("hay 5");
        assertThat(estadosDeReserva(venta)).as("no se apartó nada").isEmpty();
    }

    @Test
    @DisplayName("Criterio 3: si Inventario no responde, al vencer el timeout el barrido compensa la saga")
    void sinRespuestaElBarridoCompensa() {
        RabbitListenerEndpointRegistry oidosDeInventario =
                inventario.getBean(RabbitListenerEndpointRegistry.class);
        oidosDeInventario.stop();
        try {
            UUID venta = ventaPor("1");
            confirmar(venta);
            await().atMost(TOPE).until(() -> publicado("ventas", venta, "solicitar_reserva_stock"));
            assertThat(estadoVenta(venta)).isEqualTo("PENDIENTE_STOCK");

            // Pasa el tiempo: el timeout de la saga vence sin respuesta.
            comoSuperusuario("regenta_ventas", "update ventas.sagas set timeout_en = now() - interval '1 minute' "
                    + "where agregado_id = '" + venta + "'");
            int compensadas = enNegocio(Set.of(),
                    () -> ventas.getBean(SagaDeConfirmacionDeVenta.class).compensarVencidas());

            assertThat(compensadas).isEqualTo(1);
            assertThat(estadoVenta(venta)).isEqualTo("BORRADOR");
            assertThat(estadoSaga(venta)).isEqualTo("COMPENSADA");

            // Inventario vuelve y contesta tarde: la venta compensada no revive.
            oidosDeInventario.start();
            await().atMost(TOPE).until(() -> publicado("inventario", venta, "stock_reservado"));
            await().during(Duration.ofSeconds(2)).atMost(TOPE)
                    .until(() -> estadoVenta(venta).equals("BORRADOR"));
            assertThat(contarEventos("ventas", venta, "venta_completada")).isZero();
        } finally {
            if (!oidosDeInventario.isRunning()) {
                oidosDeInventario.start();
            }
        }
    }

    @Test
    @DisplayName("Criterio 4: el evento entregado dos veces no reserva dos veces ni publica una segunda venta_completada")
    void elEventoRepetidoNoDuplica() {
        UUID venta = ventaPor("2");
        confirmar(venta);
        await().atMost(TOPE).until(() -> estadoVenta(venta).equals("CONFIRMADA"));
        await().atMost(TOPE).until(() -> estadosDeReserva(venta).equals(List.of("CONFIRMADA")));

        // RabbitMQ entrega at-least-once: llegan otra vez, con el mismo message-id,
        // la solicitud de reserva y la respuesta de Inventario.
        reenviar("ventas", venta, "solicitar_reserva_stock");
        reenviar("inventario", venta, "stock_reservado");

        await().during(Duration.ofSeconds(2)).atMost(TOPE).until(() ->
                estadosDeReserva(venta).size() == 1
                        && contarEventos("ventas", venta, "venta_completada") == 1
                        && contarEventos("inventario", venta, "stock_reservado") == 1);
        assertThat(cantidadReservada()).isEqualByComparingTo("0");
        assertThat(consultar("inventario", "select count(*) from inventario.movimientos_inventario "
                + "where origen_id = '" + venta + "' and tipo = 'SALIDA_VENTA'"))
                .as("la salida real se registró una sola vez").containsExactly("1");
    }

    // ------------------------------------------------------------------ apoyo

    private <T> T enNegocio(Set<String> permisos, Supplier<T> tarea) {
        AtomicReference<T> resultado = new AtomicReference<>();
        ContextoDeNegocio.en(new DatosDelNegocio(negocio, usuario, "PROFESIONAL", "VENTA_DIRECTA",
                Set.of("ADMINISTRADOR"), Set.of("VENTAS", "INVENTARIO"), permisos, Set.of()),
                () -> resultado.set(tarea.get()));
        return resultado.get();
    }

    /** Reenvía al broker el mensaje tal como salió del outbox: mismo message-id, mismas cabeceras. */
    private void reenviar(String servicio, UUID venta, String tipoEvento) {
        List<String> fila = consultarFila(servicio, "select id::text, payload::text from " + servicio
                + ".outbox_eventos where tipo_evento = '" + tipoEvento + "' and payload::text like '%"
                + venta + "%'");
        MessageProperties props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        props.setMessageId(fila.get(0));
        props.setHeader("negocio_id", negocio.toString());
        props.setHeader("tipo_evento", tipoEvento);
        ventas.getBean(RabbitTemplate.class).send("regenta.eventos", tipoEvento,
                new Message(fila.get(1).getBytes(StandardCharsets.UTF_8), props));
    }

    private String estadoVenta(UUID venta) {
        return consultar("ventas", "select estado from ventas.ventas where id = '" + venta + "'").get(0);
    }

    private String estadoSaga(UUID venta) {
        return consultar("ventas", "select estado from ventas.sagas where agregado_id = '" + venta + "'").get(0);
    }

    private List<String> estadosDeReserva(UUID venta) {
        return consultar("inventario", "select estado from inventario.reservas_stock where origen_id = '"
                + venta + "'");
    }

    private BigDecimal cantidadReservada() {
        return new BigDecimal(consultar("inventario", "select cantidad_reservada from inventario.existencias "
                + "where producto_id = '" + producto + "' and bodega_id = '" + bodega + "'").get(0));
    }

    private boolean publicado(String servicio, UUID venta, String tipoEvento) {
        return !consultar(servicio, "select 1 from " + servicio + ".outbox_eventos where tipo_evento = '"
                + tipoEvento + "' and estado = 'PUBLICADO' and payload::text like '%" + venta + "%'").isEmpty();
    }

    private long contarEventos(String servicio, UUID venta, String tipoEvento) {
        return Long.parseLong(consultar(servicio, "select count(*) from " + servicio + ".outbox_eventos "
                + "where tipo_evento = '" + tipoEvento + "' and payload::text like '%" + venta + "%'").get(0));
    }

    private static String url(String base) {
        return "jdbc:postgresql://" + PG.getHost() + ":" + PG.getMappedPort(5432) + "/" + base;
    }

    private static void comoSuperusuario(String base, String sql) {
        try (Connection c = DriverManager.getConnection(url(base), PG.getUsername(), PG.getPassword());
                Statement s = c.createStatement()) {
            s.execute(sql);
        } catch (SQLException fallo) {
            throw new IllegalStateException("Falló: " + sql, fallo);
        }
    }

    /** Lee como superusuario: las aserciones miran filas por id, de un solo negocio. */
    private static List<String> consultar(String servicio, String sql) {
        List<String> filas = new ArrayList<>();
        try (Connection c = DriverManager.getConnection(url("regenta_" + servicio), PG.getUsername(),
                PG.getPassword()); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                filas.add(rs.getString(1));
            }
        } catch (SQLException fallo) {
            throw new IllegalStateException("Falló: " + sql, fallo);
        }
        return filas;
    }

    private static List<String> consultarFila(String servicio, String sql) {
        try (Connection c = DriverManager.getConnection(url("regenta_" + servicio), PG.getUsername(),
                PG.getPassword()); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            if (!rs.next()) {
                throw new IllegalStateException("Sin filas: " + sql);
            }
            List<String> fila = new ArrayList<>();
            for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                fila.add(rs.getString(i));
            }
            return fila;
        } catch (SQLException fallo) {
            throw new IllegalStateException("Falló: " + sql, fallo);
        }
    }
}
