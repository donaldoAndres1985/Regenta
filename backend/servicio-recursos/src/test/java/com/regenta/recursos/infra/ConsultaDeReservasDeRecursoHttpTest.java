package com.regenta.recursos.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.regenta.comun.errores.ServicioNoDisponibleException;
import com.regenta.comun.errores.SinPermisoException;
import com.regenta.comun.negocio.CabecerasDeNegocio;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.DatosDelNegocio;
import com.regenta.recursos.aplicacion.ConsultaDeReservasDeRecurso;
import com.regenta.recursos.aplicacion.ReservaAfectada;

/**
 * HU-138 criterios 4 y 5. servicio-recursos le pregunta a servicio-reservas si
 * un recurso tiene reservas por delante (antes de borrarlo) y qué reservas
 * confirmadas pisa un bloqueo, en vez de un stub. servicio-reservas se simula
 * con el contrato de {@code GET /api/reservas/de-recurso/{recursoId}}.
 */
class ConsultaDeReservasDeRecursoHttpTest {

    private static final String RESERVAS = "http://servicio-reservas";

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();
    private final UUID recurso = UUID.randomUUID();

    private static final OffsetDateTime DESDE = OffsetDateTime.parse("2026-06-10T00:00:00Z");
    private static final OffsetDateTime HASTA = OffsetDateTime.parse("2026-06-12T00:00:00Z");

    private MockRestServiceServer reservas;
    private ConsultaDeReservasDeRecurso consulta;

    @BeforeEach
    void reservasSimulado() {
        RestClient.Builder builder = RestClient.builder();
        reservas = MockRestServiceServer.bindTo(builder).build();
        consulta = new ConsultaDeReservasDeRecursoHttp(builder, RESERVAS);
    }

    private <T> T comoAdminDe(UUID negocio, Supplier<T> tarea) {
        AtomicReference<T> resultado = new AtomicReference<>();
        ContextoDeNegocio.en(new DatosDelNegocio(negocio, admin, "BASICO", "RESERVA",
                Set.of("ADMINISTRADOR"), Set.of("RECURSOS", "RESERVAS"),
                Set.of("RECURSOS_RECURSO_ELIMINAR", "RESERVAS_RESERVA_VER"), Set.of()),
                () -> resultado.set(tarea.get()));
        return resultado.get();
    }

    private static String iso(OffsetDateTime instante) {
        return URLEncoder.encode(DateTimeFormatter.ISO_INSTANT.format(instante),
                StandardCharsets.UTF_8);
    }

    private String reservaJson(String numero, String desde, String hasta) {
        return """
                {"id":"%s","numero":"%s","estado":"CONFIRMADA","desde":"%s","hasta":"%s"}
                """.formatted(UUID.randomUUID(), numero, desde, hasta);
    }

    @Test
    @DisplayName("Criterio 4: con alguna reserva que ocupa el recurso de ahora en adelante, tiene reservas futuras")
    void conReservasFuturas() {
        reservas.expect(requestTo(Matchers.startsWith(
                RESERVAS + "/api/reservas/de-recurso/" + recurso + "?desde=")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("soloConfirmadas", "false"))
                .andExpect(peticion -> assertThat(peticion.getURI().getQuery()).doesNotContain("hasta"))
                .andRespond(withSuccess("[" + reservaJson("R-0001", "2026-06-20T15:00:00Z",
                        "2026-06-22T11:00:00Z") + "]", MediaType.APPLICATION_JSON));

        assertThat(comoAdminDe(negocioA, () -> consulta.tieneReservasFuturas(negocioA, recurso)))
                .isTrue();
        reservas.verify();
    }

    @Test
    @DisplayName("Criterio 4: sin reservas por delante, el recurso no tiene reservas futuras")
    void sinReservasFuturas() {
        reservas.expect(requestTo(Matchers.startsWith(
                RESERVAS + "/api/reservas/de-recurso/" + recurso)))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(comoAdminDe(negocioA, () -> consulta.tieneReservasFuturas(negocioA, recurso)))
                .isFalse();
    }

    @Test
    @DisplayName("Criterio 5: las reservas confirmadas que pisa el bloqueo, con su número y su estadía")
    void confirmadasQuePisaElBloqueo() {
        reservas.expect(requestTo(RESERVAS + "/api/reservas/de-recurso/" + recurso + "?desde="
                + iso(DESDE) + "&hasta=" + iso(HASTA) + "&soloConfirmadas=true"))
                .andRespond(withSuccess("[" + reservaJson("R-0042", "2026-06-11T15:00:00Z",
                        "2026-06-13T11:00:00Z") + "]", MediaType.APPLICATION_JSON));

        var afectadas = comoAdminDe(negocioA,
                () -> consulta.reservasConfirmadasEnPeriodo(negocioA, recurso, DESDE, HASTA));

        assertThat(afectadas).singleElement().satisfies(r -> {
            assertThat(r.codigo()).isEqualTo("R-0042");
            assertThat(r.reservaId()).isNotNull();
            assertThat(r.entrada()).isEqualTo(OffsetDateTime.parse("2026-06-11T15:00:00Z"));
            assertThat(r.salida()).isEqualTo(OffsetDateTime.parse("2026-06-13T11:00:00Z"));
        });
        // servicio-reservas no guarda el nombre del huésped, solo su cliente.
        assertThat(afectadas).extracting(ReservaAfectada::huesped).containsOnlyNulls();
    }

    @Test
    @DisplayName("Criterio 9: la consulta viaja con el negocio de quien la hace")
    void viajaConElNegocioDeQuienPregunta() {
        reservas.expect(requestTo(Matchers.startsWith(
                RESERVAS + "/api/reservas/de-recurso/" + recurso)))
                .andExpect(header(CabecerasDeNegocio.NEGOCIO, negocioB.toString()))
                .andExpect(header(CabecerasDeNegocio.USUARIO, admin.toString()))
                .andExpect(peticion -> assertThat(
                        peticion.getHeaders().getFirst(CabecerasDeNegocio.PERMISOS))
                        .contains("RESERVAS_RESERVA_VER"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(comoAdminDe(negocioB, () -> consulta.tieneReservasFuturas(negocioB, recurso)))
                .isFalse();
        reservas.verify();
    }

    @Test
    @DisplayName("Criterio 10: si reservas no responde, no se asume que no hay reservas: 503")
    void reservasCaido() {
        reservas.expect(requestTo(Matchers.startsWith(RESERVAS + "/api/reservas/de-recurso/")))
                .andRespond(withServerError());
        reservas.expect(requestTo(Matchers.startsWith(RESERVAS + "/api/reservas/de-recurso/")))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> comoAdminDe(negocioA,
                () -> consulta.tieneReservasFuturas(negocioA, recurso)))
                .isInstanceOf(ServicioNoDisponibleException.class)
                .hasMessageContaining("reservas");
        assertThatThrownBy(() -> comoAdminDe(negocioA,
                () -> consulta.reservasConfirmadasEnPeriodo(negocioA, recurso, DESDE, HASTA)))
                .isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    @DisplayName("Sin permiso de ver reservas, servicio-reservas responde 403 y no se sigue")
    void sinPermiso() {
        reservas.expect(requestTo(Matchers.startsWith(RESERVAS + "/api/reservas/de-recurso/")))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> comoAdminDe(negocioA,
                () -> consulta.tieneReservasFuturas(negocioA, recurso)))
                .isInstanceOf(SinPermisoException.class);
    }
}
