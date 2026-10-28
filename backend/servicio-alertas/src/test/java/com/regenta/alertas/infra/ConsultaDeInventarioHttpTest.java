package com.regenta.alertas.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.regenta.alertas.aplicacion.ConsultaDeInventario;
import com.regenta.alertas.aplicacion.ConsultaDeInventario.LotePorVencer;
import com.regenta.comun.errores.ServicioNoDisponibleException;
import com.regenta.comun.negocio.CabecerasDeNegocio;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.DatosDelNegocio;

/**
 * HU-138 criterio 7. El barrido de vencimientos (HU-093 criterio 2) le pregunta
 * a servicio-inventario qué lotes están por vencer, en vez de a un stub. Corre
 * sin usuario: la consulta va como SISTEMA con solo INVENTARIO_LOTE_VER.
 */
class ConsultaDeInventarioHttpTest {

    private static final String INVENTARIO = "http://servicio-inventario";

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();

    private MockRestServiceServer inventario;
    private ConsultaDeInventario consulta;

    @BeforeEach
    void inventarioSimulado() {
        RestClient.Builder builder = RestClient.builder();
        inventario = MockRestServiceServer.bindTo(builder).build();
        consulta = new ConsultaDeInventarioHttp(builder, INVENTARIO);
    }

    /** El contexto con que corre un barrido: rol SISTEMA, sin usuario ni permisos. */
    private <T> T enBarridoDe(UUID negocio, Supplier<T> tarea) {
        AtomicReference<T> resultado = new AtomicReference<>();
        ContextoDeNegocio.en(new DatosDelNegocio(negocio, null, "", "", Set.of("SISTEMA"),
                Set.of("ALERTAS"), Set.of(), Set.of()), () -> resultado.set(tarea.get()));
        return resultado.get();
    }

    @Test
    @DisplayName("Criterio 7: el barrido encuentra en inventario los lotes que vencen dentro de la ventana")
    void lotesReales() {
        UUID lote = UUID.randomUUID();
        UUID yogur = UUID.randomUUID();
        LocalDate vence = LocalDate.of(2026, 11, 5);
        inventario.expect(requestTo(INVENTARIO + "/api/inventario/lotes/por-vencer?ventanaDias=15"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [{"loteId":"%s","productoId":"%s","productoNombre":"Yogur",
                          "codigoLote":"L-10","fechaVencimiento":"2026-11-05","diasParaVencer":9,
                          "existencia":10.000000}]
                        """.formatted(lote, yogur), MediaType.APPLICATION_JSON));

        List<LotePorVencer> lotes = enBarridoDe(negocioA, () -> consulta.lotesPorVencer(negocioA, 15));

        assertThat(lotes).singleElement().satisfies(l -> {
            assertThat(l.loteId()).isEqualTo(lote);
            assertThat(l.productoId()).isEqualTo(yogur);
            assertThat(l.productoNombre()).isEqualTo("Yogur");
            assertThat(l.codigoLote()).isEqualTo("L-10");
            assertThat(l.diasParaVencer()).isEqualTo(9);
            assertThat(l.fechaVencimiento()).isEqualTo(vence);
            assertThat(l.existencia()).isEqualByComparingTo("10");
        });
        inventario.verify();
    }

    @Test
    @DisplayName("Criterio 7: el barrido consulta como SISTEMA, con solo el permiso de ver lotes")
    void comoSistema() {
        inventario.expect(requestTo(INVENTARIO + "/api/inventario/lotes/por-vencer?ventanaDias=15"))
                .andExpect(header(CabecerasDeNegocio.ROLES, "SISTEMA"))
                .andExpect(header(CabecerasDeNegocio.MODULOS, "INVENTARIO"))
                .andExpect(header(CabecerasDeNegocio.PERMISOS, "INVENTARIO_LOTE_VER"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        enBarridoDe(negocioA, () -> consulta.lotesPorVencer(negocioA, 15));
        inventario.verify();
    }

    @Test
    @DisplayName("Criterio 9: cada negocio consulta sus propios lotes")
    void viajaConElNegocio() {
        inventario.expect(requestTo(INVENTARIO + "/api/inventario/lotes/por-vencer?ventanaDias=15"))
                .andExpect(header(CabecerasDeNegocio.NEGOCIO, negocioB.toString()))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(enBarridoDe(negocioB, () -> consulta.lotesPorVencer(negocioB, 15))).isEmpty();
        inventario.verify();
    }

    @Test
    @DisplayName("Criterio 10: si inventario no responde, el barrido falla en vez de concluir que no vence nada")
    void inventarioCaido() {
        inventario.expect(requestTo(INVENTARIO + "/api/inventario/lotes/por-vencer?ventanaDias=15"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> enBarridoDe(negocioA, () -> consulta.lotesPorVencer(negocioA, 15)))
                .isInstanceOf(ServicioNoDisponibleException.class)
                .hasMessageContaining("inventario");
    }
}
