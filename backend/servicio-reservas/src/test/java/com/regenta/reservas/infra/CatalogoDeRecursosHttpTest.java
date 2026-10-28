package com.regenta.reservas.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ServicioNoDisponibleException;
import com.regenta.comun.negocio.CabecerasDeNegocio;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.DatosDelNegocio;
import com.regenta.reservas.aplicacion.AnticipoRequerido;
import com.regenta.reservas.aplicacion.BloqueoEnCalendario;
import com.regenta.reservas.aplicacion.CatalogoDeRecursos;
import com.regenta.reservas.aplicacion.CotizacionDeEstadia;
import com.regenta.reservas.aplicacion.PenalizacionDeCancelacion;
import com.regenta.reservas.aplicacion.RecursoReservable;

/**
 * HU-138 criterio 3. servicio-reservas le pregunta a servicio-recursos por los
 * recursos reservables con el buffer de su tipo, los bloqueos, la tarifa y la
 * política de cancelación, en vez de un stub. servicio-recursos se simula con
 * su contrato HTTP (HU-064 a HU-068) y dos negocios.
 */
class CatalogoDeRecursosHttpTest {

    private static final String RECURSOS = "http://servicio-recursos";

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID recepcion = UUID.randomUUID();
    private final UUID habitaciones = UUID.randomUUID();
    private final UUID canchas = UUID.randomUUID();
    private final UUID r101 = UUID.randomUUID();
    private final UUID r102 = UUID.randomUUID();
    private final UUID cancha1 = UUID.randomUUID();
    private final UUID politica = UUID.randomUUID();

    private static final OffsetDateTime DESDE = OffsetDateTime.of(2026, 12, 20, 15, 0, 0, 0,
            ZoneOffset.UTC);
    private static final OffsetDateTime HASTA = DESDE.plusDays(3);

    private MockRestServiceServer recursos;
    private CatalogoDeRecursos catalogo;

    @BeforeEach
    void recursosSimulado() {
        RestClient.Builder builder = RestClient.builder();
        recursos = MockRestServiceServer.bindTo(builder).build();
        catalogo = new CatalogoDeRecursosHttp(builder, RECURSOS);
    }

    private <T> T comoRecepcionDe(UUID negocio, Supplier<T> tarea) {
        AtomicReference<T> resultado = new AtomicReference<>();
        ContextoDeNegocio.en(new DatosDelNegocio(negocio, recepcion, "BASICO", "RESERVA",
                Set.of("RECEPCIONISTA"), Set.of("RESERVAS", "RECURSOS"),
                Set.of("RESERVAS_RESERVA_CREAR", "RECURSOS_RECURSO_VER"), Set.of()),
                () -> resultado.set(tarea.get()));
        return resultado.get();
    }

    private static String fecha(OffsetDateTime instante) {
        return URLEncoder.encode(java.time.format.DateTimeFormatter.ISO_INSTANT.format(instante),
                StandardCharsets.UTF_8);
    }

    private String recursosJson() {
        return """
                [{"id":"%s","tipoRecursoId":"%s","codigo":"101","nombre":"Habitación 101",
                  "descripcion":null,"sucursalId":null,"capacidad":2,"piso":"1","zona":null,
                  "estado":"DISPONIBLE","disponible":true,"atributos":{},"imagenUrl":null},
                 {"id":"%s","tipoRecursoId":"%s","codigo":"102","nombre":"Habitación 102",
                  "descripcion":null,"sucursalId":null,"capacidad":4,"piso":"1","zona":null,
                  "estado":"DISPONIBLE","disponible":true,"atributos":{},"imagenUrl":null},
                 {"id":"%s","tipoRecursoId":"%s","codigo":"C1","nombre":"Cancha 1",
                  "descripcion":null,"sucursalId":null,"capacidad":10,"piso":null,"zona":null,
                  "estado":"DISPONIBLE","disponible":true,"atributos":{},"imagenUrl":null}]
                """.formatted(r101, habitaciones, r102, habitaciones, cancha1, canchas);
    }

    private String tiposJson() {
        return """
                [{"id":"%s","nombre":"Habitación","descripcion":null,"unidadTiempo":"NOCHE",
                  "duracionMinimaMin":1440,"incrementoMin":1440,"capacidadDefault":2,
                  "permiteOverbooking":false,"bufferAntesMin":60,"bufferDespuesMin":120,
                  "activo":true,"atributos":[]},
                 {"id":"%s","nombre":"Cancha","descripcion":null,"unidadTiempo":"HORA",
                  "duracionMinimaMin":60,"incrementoMin":30,"capacidadDefault":10,
                  "permiteOverbooking":false,"bufferAntesMin":0,"bufferDespuesMin":15,
                  "activo":true,"atributos":[]}]
                """.formatted(habitaciones, canchas);
    }

    private void esperaRecursosYTipos() {
        recursos.expect(requestTo(RECURSOS + "/api/recursos?soloDisponibles=true"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(recursosJson(), MediaType.APPLICATION_JSON));
        recursos.expect(requestTo(RECURSOS + "/api/recursos/tipos"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(tiposJson(), MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Criterio 3: los reservables son los recursos disponibles, con el buffer de su tipo")
    void reservablesConElBufferDeSuTipo() {
        esperaRecursosYTipos();

        var todos = comoRecepcionDe(negocioA, () -> catalogo.reservables(negocioA, null));

        assertThat(todos).extracting(RecursoReservable::id).containsExactly(r101, r102, cancha1);
        assertThat(todos.get(0)).isEqualTo(new RecursoReservable(r101, habitaciones, "101",
                "Habitación 101", 2, 60, 120));
        assertThat(todos.get(2).bufferAntesMin()).isZero();
        assertThat(todos.get(2).bufferDespuesMin()).isEqualTo(15);
        recursos.verify();
    }

    @Test
    @DisplayName("Criterio 3: con un tipo, solo los recursos de ese tipo")
    void reservablesDeUnTipo() {
        esperaRecursosYTipos();

        var soloHabitaciones = comoRecepcionDe(negocioA,
                () -> catalogo.reservables(negocioA, habitaciones));

        assertThat(soloHabitaciones).extracting(RecursoReservable::id).containsExactly(r101, r102);
    }

    @Test
    @DisplayName("Criterio 3: los bloqueos del periodo, con su tramo y su motivo, y los recursos bloqueados")
    void bloqueosDelPeriodo() {
        String bloqueos = """
                [{"id":"%s","recursoId":"%s","desde":"2026-12-21T00:00:00Z",
                  "hasta":"2026-12-22T00:00:00Z","motivo":"MANTENIMIENTO","detalle":"Pintura",
                  "usuarioId":null,"creadoEn":"2026-12-01T10:00:00Z"},
                 {"id":"%s","recursoId":"%s","desde":"2026-12-22T00:00:00Z",
                  "hasta":"2026-12-23T00:00:00Z","motivo":"LIMPIEZA_PROFUNDA","detalle":null,
                  "usuarioId":null,"creadoEn":"2026-12-01T10:00:00Z"}]
                """.formatted(UUID.randomUUID(), r101, UUID.randomUUID(), r101);
        recursos.expect(ExpectedCount.twice(), requestTo(RECURSOS + "/api/recursos/bloqueos?desde="
                + fecha(DESDE) + "&hasta=" + fecha(HASTA)))
                .andRespond(withSuccess(bloqueos, MediaType.APPLICATION_JSON));

        var enCalendario = comoRecepcionDe(negocioA,
                () -> catalogo.bloqueosEnPeriodo(negocioA, DESDE, HASTA));
        var bloqueados = comoRecepcionDe(negocioA,
                () -> catalogo.recursosBloqueados(negocioA, DESDE, HASTA));

        assertThat(enCalendario).first().isEqualTo(new BloqueoEnCalendario(r101,
                OffsetDateTime.parse("2026-12-21T00:00:00Z"),
                OffsetDateTime.parse("2026-12-22T00:00:00Z"), "MANTENIMIENTO"));
        assertThat(enCalendario).hasSize(2);
        assertThat(bloqueados).containsExactly(r101);
        recursos.verify();
    }

    @Test
    @DisplayName("Criterio 3: la estadía se cotiza con las tarifas reales, noche por noche")
    void cotizaConLasTarifasReales() {
        UUID temporadaAlta = UUID.randomUUID();
        recursos.expect(requestTo(RECURSOS + "/api/recursos/cotizaciones"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"recursoId":"%s","fechaEntrada":"2026-12-20","noches":2,"personas":2}
                        """.formatted(r101)))
                .andRespond(withSuccess("""
                        {"recursoId":"%s","personas":2,"total":560000.00,"moneda":"COP",
                         "completa":true,"noches":[
                           {"fecha":"2026-12-20","tarifaId":"%s","tarifaNombre":"Temporada alta",
                            "prioridad":10,"precio":280000.00},
                           {"fecha":"2026-12-21","tarifaId":"%s","tarifaNombre":"Temporada alta",
                            "prioridad":10,"precio":280000.00}]}
                        """.formatted(r101, temporadaAlta, temporadaAlta),
                        MediaType.APPLICATION_JSON));

        CotizacionDeEstadia cot = comoRecepcionDe(negocioA,
                () -> catalogo.cotizar(negocioA, r101, LocalDate.of(2026, 12, 20), 2, 2));

        assertThat(cot.total()).isEqualByComparingTo("560000");
        assertThat(cot.moneda()).isEqualTo("COP");
        assertThat(cot.completa()).isTrue();
        assertThat(cot.noches()).hasSize(2);
        assertThat(cot.noches().get(1)).satisfies(n -> {
            assertThat(n.fecha()).isEqualTo(LocalDate.of(2026, 12, 21));
            assertThat(n.tarifaId()).isEqualTo(temporadaAlta);
            assertThat(n.precio()).isEqualByComparingTo("280000");
        });
        assertThat(cot.tarifaDeCabecera()).isEqualTo(temporadaAlta);
        recursos.verify();
    }

    @Test
    @DisplayName("Criterio 3: el anticipo y la penalización salen de la política de cancelación real")
    void anticipoYPenalizacionDeLaPolitica() {
        String aplicacion = """
                {"politicaId":"%s","politicaNombre":"Flexible 48 h","horasAntes":48,
                 "montoReserva":560000.00,"anticipoRequerido":168000.00,"penalizacion":%s,
                 "horasDeAntelacion":%s,"dentroDePlazo":%s}
                """;
        recursos.expect(requestTo(RECURSOS + "/api/recursos/politicas-cancelacion/aplicacion"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"politicaId":"%s","montoReserva":560000,"entrada":"2026-12-20T15:00:00Z"}
                        """.formatted(politica)))
                .andRespond(withSuccess(aplicacion.formatted(politica, "0", "200", "true"),
                        MediaType.APPLICATION_JSON));
        recursos.expect(requestTo(RECURSOS + "/api/recursos/politicas-cancelacion/aplicacion"))
                .andRespond(withSuccess(aplicacion.formatted(politica, "280000.00", "10", "false"),
                        MediaType.APPLICATION_JSON));

        AnticipoRequerido anticipo = comoRecepcionDe(negocioA, () -> catalogo.anticipo(negocioA,
                politica, new BigDecimal("560000"), DESDE));
        PenalizacionDeCancelacion penalizacion = comoRecepcionDe(negocioA,
                () -> catalogo.penalizacionPorCancelar(negocioA, politica,
                        new BigDecimal("560000"), DESDE));

        assertThat(anticipo.politicaCancelacionId()).isEqualTo(politica);
        assertThat(anticipo.anticipo()).isEqualByComparingTo("168000");
        assertThat(penalizacion.penalizacion()).isEqualByComparingTo("280000");
        assertThat(penalizacion.dentroDePlazo()).isFalse();
        recursos.verify();
    }

    @Test
    @DisplayName("Criterio 3: un negocio sin política de cancelación por defecto no exige anticipo ni cobra penalización")
    void sinPoliticaPorDefecto() {
        recursos.expect(ExpectedCount.twice(),
                requestTo(RECURSOS + "/api/recursos/politicas-cancelacion/aplicacion"))
                .andRespond(withResourceNotFound());

        assertThat(comoRecepcionDe(negocioA, () -> catalogo.anticipo(negocioA, null,
                new BigDecimal("560000"), DESDE))).isEqualTo(AnticipoRequerido.ninguno());
        assertThat(comoRecepcionDe(negocioA, () -> catalogo.penalizacionPorCancelar(negocioA, null,
                new BigDecimal("560000"), DESDE))).isEqualTo(PenalizacionDeCancelacion.sinCosto());
    }

    @Test
    @DisplayName("Criterio 3: una política elegida que no existe no se reemplaza por ninguna")
    void politicaElegidaInexistente() {
        recursos.expect(requestTo(RECURSOS + "/api/recursos/politicas-cancelacion/aplicacion"))
                .andRespond(withResourceNotFound());

        assertThatThrownBy(() -> comoRecepcionDe(negocioA, () -> catalogo.anticipo(negocioA,
                politica, new BigDecimal("560000"), DESDE)))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    @DisplayName("Criterio 9: cada consulta viaja con el negocio de quien pregunta")
    void viajaConElNegocioDeQuienPregunta() {
        recursos.expect(requestTo(RECURSOS + "/api/recursos?soloDisponibles=true"))
                .andExpect(header(CabecerasDeNegocio.NEGOCIO, negocioB.toString()))
                .andExpect(header(CabecerasDeNegocio.USUARIO, recepcion.toString()))
                .andExpect(peticion -> assertThat(
                        peticion.getHeaders().getFirst(CabecerasDeNegocio.PERMISOS))
                        .contains("RECURSOS_RECURSO_VER"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        recursos.expect(requestTo(RECURSOS + "/api/recursos/tipos"))
                .andExpect(header(CabecerasDeNegocio.NEGOCIO, negocioB.toString()))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(comoRecepcionDe(negocioB, () -> catalogo.reservables(negocioB, null))).isEmpty();
        recursos.verify();
    }

    @Test
    @DisplayName("Criterio 10: si recursos no responde, la disponibilidad falla con un error claro, no con una lista vacía")
    void recursosCaido() {
        recursos.expect(requestTo(RECURSOS + "/api/recursos?soloDisponibles=true"))
                .andRespond(withServerError());
        recursos.expect(requestTo(RECURSOS + "/api/recursos/bloqueos?desde=" + fecha(DESDE)
                + "&hasta=" + fecha(HASTA)))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withStatus(HttpStatus.BAD_GATEWAY));
        recursos.expect(requestTo(RECURSOS + "/api/recursos/cotizaciones"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> comoRecepcionDe(negocioA, () -> catalogo.reservables(negocioA,
                null))).isInstanceOf(ServicioNoDisponibleException.class)
                .hasMessageContaining("recursos");
        assertThatThrownBy(() -> comoRecepcionDe(negocioA, () -> catalogo.recursosBloqueados(
                negocioA, DESDE, HASTA))).isInstanceOf(ServicioNoDisponibleException.class);
        assertThatThrownBy(() -> comoRecepcionDe(negocioA, () -> catalogo.cotizar(negocioA, r101,
                LocalDate.of(2026, 12, 20), 2, 2)))
                .isInstanceOf(ServicioNoDisponibleException.class);
    }
}
