package com.regenta.gateway.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

/**
 * HU-007. El gateway se prueba con un servicio destino de verdad -- un servidor
 * minimo que solo apunta lo que recibe -- porque la mitad de los criterios no
 * hablan de la respuesta sino de si la peticion llego o no al servicio.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayDeSeguridadTest {

    private static final String SECRETO = "secreto-de-pruebas-de-regenta-con-mas-de-32-caracteres";
    private static final String OTRO_SECRETO = "otro-secreto-que-no-es-el-nuestro-pero-igual-de-largo";
    private static final String NEGOCIO = "9f1c6b0e-3a2d-4c58-9d21-5b7a0e4f1c33";
    private static final String USUARIO = "11111111-2222-3333-4444-555555555555";

    /** Lo ultimo que vio el servicio destino, en minusculas para no pelear con el case. */
    private static final Map<String, String> CABECERAS_RECIBIDAS = new ConcurrentHashMap<>();
    private static final AtomicInteger LLAMADAS = new AtomicInteger();

    private static final DisposableServer SERVICIO_DESTINO = HttpServer.create()
            .port(0)
            .handle((peticion, respuesta) -> {
                LLAMADAS.incrementAndGet();
                CABECERAS_RECIBIDAS.clear();
                peticion.requestHeaders().forEach(cabecera ->
                        CABECERAS_RECIBIDAS.put(cabecera.getKey().toLowerCase(Locale.ROOT), cabecera.getValue()));
                return respuesta.status(200)
                        .header("Content-Type", "application/json")
                        .sendString(Mono.just("{\"ok\":true}"));
            })
            .bindNow();

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        String destino = "http://localhost:" + SERVICIO_DESTINO.port();
        registro.add("SERVICIO_VENTAS_URI", () -> destino);
        registro.add("JWT_SECRETO", () -> SECRETO);
    }

    @LocalServerPort
    private int puerto;

    @Autowired
    private RouteLocator rutas;

    private WebTestClient cliente;

    @BeforeEach
    void preparar() {
        LLAMADAS.set(0);
        CABECERAS_RECIBIDAS.clear();
        cliente = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + puerto)
                .responseTimeout(Duration.ofSeconds(15))
                .build();
    }

    @Test
    @DisplayName("Criterio 1: sin token responde 401 y el servicio destino ni se entera")
    void sinTokenResponde401SinTocarElServicio() {
        cliente.get().uri("/api/ventas/comprobante/1")
                .exchange()
                .expectStatus().isUnauthorized();

        assertThat(LLAMADAS.get()).isZero();
    }

    @Test
    @DisplayName("Criterio 2: con token valido enruta y propaga negocio, plan, patron y roles")
    void tokenValidoEnrutaYPropagaElContexto() throws Exception {
        cliente.get().uri("/api/ventas/comprobante/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(constructor -> { }))
                .exchange()
                .expectStatus().isOk();

        assertThat(LLAMADAS.get()).isEqualTo(1);
        assertThat(CABECERAS_RECIBIDAS)
                .containsEntry("x-regenta-negocio", NEGOCIO)
                .containsEntry("x-regenta-usuario", USUARIO)
                .containsEntry("x-regenta-plan", "PRO")
                .containsEntry("x-regenta-patron", "VENTA_DIRECTA")
                .containsEntry("x-regenta-roles", "ADMINISTRADOR,CAJERO")
                .containsEntry("x-regenta-modulos", "VENTAS,INVENTARIO")
                .containsEntry("x-regenta-permisos", "VENTAS_VENTA_CREAR");
        assertThat(CABECERAS_RECIBIDAS.get("x-regenta-traza")).isNotBlank();
        assertThat(CABECERAS_RECIBIDAS.get("authorization")).startsWith("Bearer ");
    }

    @Test
    @DisplayName("Criterio 3: token expirado responde 401 y queda en el log con su trace_id")
    void tokenExpiradoResponde401YQuedaEnElLog() throws Exception {
        ch.qos.logback.classic.Logger registro =
                (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(ConfiguracionDeSeguridad.class);
        ListAppender<ILoggingEvent> apuntes = new ListAppender<>();
        apuntes.start();
        registro.addAppender(apuntes);

        String traza = "traza-de-prueba-expirado";
        try {
            String expirado = token(constructor -> constructor
                    .issueTime(Date.from(Instant.now().minusSeconds(3600)))
                    .expirationTime(Date.from(Instant.now().minusSeconds(1800))));

            cliente.get().uri("/api/ventas/comprobante/1")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + expirado)
                    .header(Trazas.CABECERA, traza)
                    .exchange()
                    .expectStatus().isUnauthorized()
                    .expectHeader().valueEquals(Trazas.CABECERA, traza)
                    .expectBody().jsonPath("$.trace_id").isEqualTo(traza);

            assertThat(LLAMADAS.get()).isZero();
            assertThat(apuntes.list)
                    .anySatisfy(apunte -> assertThat(apunte.getFormattedMessage()).contains(traza));
        } finally {
            registro.detachAppender(apuntes);
        }
    }

    @Test
    @DisplayName("Criterio 3: firma de otro emisor responde 401")
    void firmaInvalidaResponde401() throws Exception {
        JWTClaimsSet declaraciones = declaraciones(constructor -> { }).build();
        SignedJWT falsificado = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), declaraciones);
        falsificado.sign(new MACSigner(OTRO_SECRETO.getBytes(StandardCharsets.UTF_8)));

        cliente.get().uri("/api/ventas/comprobante/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + falsificado.serialize())
                .exchange()
                .expectStatus().isUnauthorized();

        assertThat(LLAMADAS.get()).isZero();
    }

    @Test
    @DisplayName("Criterio 4: negocio SUSPENDIDO responde 402 y no enruta")
    void negocioSuspendidoResponde402() throws Exception {
        cliente.get().uri("/api/ventas/comprobante/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(constructor ->
                        constructor.claim("estado_negocio", "SUSPENDIDO")))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.PAYMENT_REQUIRED);

        assertThat(LLAMADAS.get()).isZero();
    }

    @Test
    @DisplayName("Criterio 4: negocio CANCELADO responde 402 y no enruta")
    void negocioCanceladoResponde402() throws Exception {
        cliente.get().uri("/api/ventas/comprobante/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(constructor ->
                        constructor.claim("estado_negocio", "CANCELADO")))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.PAYMENT_REQUIRED);

        assertThat(LLAMADAS.get()).isZero();
    }

    @Test
    @DisplayName("El contexto que manda el cliente se descarta: manda el token, no la cabecera")
    void lasCabecerasDeContextoDelClienteSeDescartan() throws Exception {
        cliente.get().uri("/api/ventas/comprobante/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(constructor -> { }))
                .header(FiltroDeContexto.NEGOCIO, "00000000-0000-0000-0000-000000000000")
                .header(FiltroDeContexto.ROLES, "SUPERADMIN")
                .exchange()
                .expectStatus().isOk();

        assertThat(CABECERAS_RECIBIDAS)
                .containsEntry("x-regenta-negocio", NEGOCIO)
                .containsEntry("x-regenta-roles", "ADMINISTRADOR,CAJERO")
                .containsEntry("x-regenta-modulos", "VENTAS,INVENTARIO")
                .containsEntry("x-regenta-permisos", "VENTAS_VENTA_CREAR");
    }

    @Test
    @DisplayName("La salud del gateway se atiende sin token")
    void laSaludEsPublica() {
        cliente.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("Terminado: una ruta declarada por servicio, ninguna de mas")
    void lasRutasSeDeclaranServicioPorServicio() {
        List<Route> declaradas = rutas.getRoutes().collectList().block(Duration.ofSeconds(10));

        assertThat(declaradas).isNotNull();
        assertThat(declaradas).extracting(Route::getId).containsExactlyInAnyOrder(
                "servicio-usuarios", "servicio-clientes", "servicio-inventario",
                "servicio-ventas", "servicio-compras", "servicio-recursos",
                "servicio-reservas", "servicio-menu", "servicio-mesas",
                "servicio-comandas", "servicio-facturacion", "servicio-caja",
                "servicio-alertas", "servicio-reportes", "servicio-auditoria");
    }

    @Test
    @DisplayName("Terminado: ningun comodin, una ruta no declarada no se enruta a nada")
    void loQueNoEstaDeclaradoNoSeEnruta() throws Exception {
        cliente.get().uri("/api/loquesea/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(constructor -> { }))
                .exchange()
                .expectStatus().isNotFound();

        assertThat(LLAMADAS.get()).isZero();
    }

    private static String token(Consumer<JWTClaimsSet.Builder> ajustes) throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), declaraciones(ajustes).build());
        jwt.sign(new MACSigner(SECRETO.getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }

    private static JWTClaimsSet.Builder declaraciones(Consumer<JWTClaimsSet.Builder> ajustes) {
        JWTClaimsSet.Builder constructor = new JWTClaimsSet.Builder()
                .issuer("regenta")
                .subject(USUARIO)
                .claim("negocio_id", NEGOCIO)
                .claim("plan", "PRO")
                .claim("patron", "VENTA_DIRECTA")
                .claim("roles", List.of("ADMINISTRADOR", "CAJERO"))
                .claim("modulos", List.of("VENTAS", "INVENTARIO"))
                .claim("permisos", List.of("VENTAS_VENTA_CREAR"))
                .claim("estado_negocio", "ACTIVO")
                .issueTime(Date.from(Instant.now().minusSeconds(30)))
                .expirationTime(Date.from(Instant.now().plusSeconds(900)));
        ajustes.accept(constructor);
        return constructor;
    }
}
