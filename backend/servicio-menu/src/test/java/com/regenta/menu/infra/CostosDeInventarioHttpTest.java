package com.regenta.menu.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
import com.regenta.menu.aplicacion.CostosDeInventario;

/**
 * HU-138 criterio 6. servicio-menu valora una receta con el último costo de
 * entrada de cada insumo, que le pregunta a servicio-inventario en vez de a un
 * stub. El recálculo corre a veces con un administrador detrás (al editar la
 * receta) y a veces sin nadie (al llegar {@code costo_producto_actualizado}):
 * en ese caso la consulta va como SISTEMA.
 */
class CostosDeInventarioHttpTest {

    private static final String INVENTARIO = "http://servicio-inventario";

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();
    private final UUID carne = UUID.randomUUID();
    private final UUID arroz = UUID.randomUUID();

    private MockRestServiceServer inventario;
    private CostosDeInventario costos;

    @BeforeEach
    void inventarioSimulado() {
        RestClient.Builder builder = RestClient.builder();
        inventario = MockRestServiceServer.bindTo(builder).build();
        costos = new CostosDeInventarioHttp(builder, INVENTARIO);
    }

    private <T> T como(DatosDelNegocio quien, Supplier<T> tarea) {
        AtomicReference<T> resultado = new AtomicReference<>();
        ContextoDeNegocio.en(quien, () -> resultado.set(tarea.get()));
        return resultado.get();
    }

    private DatosDelNegocio adminDe(UUID negocio) {
        return new DatosDelNegocio(negocio, admin, "PROFESIONAL", "COMANDA",
                Set.of("ADMINISTRADOR"), Set.of("MENU", "INVENTARIO"),
                Set.of("MENU_PLATO_EDITAR", "INVENTARIO_PRODUCTO_VER"), Set.of());
    }

    /** El contexto que arma el consumidor de {@code costo_producto_actualizado}. */
    private DatosDelNegocio listenerDe(UUID negocio) {
        return new DatosDelNegocio(negocio, null, "", "COMANDA", Set.of(), Set.of("MENU"),
                Set.of(), Set.of());
    }

    @Test
    @DisplayName("Criterio 6: la receta se valora con el último costo de entrada de cada insumo")
    void costosReales() {
        inventario.expect(requestTo(Matchers.startsWith(INVENTARIO + "/api/inventario/productos/costos")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("ids", carne.toString(), arroz.toString()))
                .andRespond(withSuccess("""
                        [{"productoId":"%s","costoUnitario":28000.0000}]
                        """.formatted(carne), MediaType.APPLICATION_JSON));

        Map<UUID, BigDecimal> porProducto = como(adminDe(negocioA),
                () -> costos.costoUnitarioDe(negocioA, List.of(carne, arroz)));

        // Un insumo sin costo conocido no aparece: la receta lo valora en cero.
        assertThat(porProducto).containsOnlyKeys(carne);
        assertThat(porProducto.get(carne)).isEqualByComparingTo("28000");
        inventario.verify();
    }

    @Test
    @DisplayName("Criterio 6: una receta sin insumos no le pregunta nada a inventario")
    void sinInsumosNoPregunta() {
        assertThat(como(adminDe(negocioA), () -> costos.costoUnitarioDe(negocioA, List.of())))
                .isEmpty();
        inventario.verify();
    }

    @Test
    @DisplayName("Criterio 6: al recalcular por un evento, sin usuario, la consulta va como SISTEMA con solo INVENTARIO_PRODUCTO_VER")
    void sinUsuarioVaComoSistema() {
        inventario.expect(requestTo(Matchers.startsWith(INVENTARIO + "/api/inventario/productos/costos")))
                .andExpect(header(CabecerasDeNegocio.ROLES, "SISTEMA"))
                .andExpect(header(CabecerasDeNegocio.MODULOS, "INVENTARIO"))
                .andExpect(header(CabecerasDeNegocio.PERMISOS, "INVENTARIO_PRODUCTO_VER"))
                .andExpect(header(CabecerasDeNegocio.NEGOCIO, negocioA.toString()))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        como(listenerDe(negocioA), () -> costos.costoUnitarioDe(negocioA, List.of(carne)));
        inventario.verify();
    }

    @Test
    @DisplayName("Criterio 9: la consulta viaja con el negocio de la receta y la identidad de quien la edita")
    void viajaConElNegocio() {
        inventario.expect(requestTo(Matchers.startsWith(INVENTARIO + "/api/inventario/productos/costos")))
                .andExpect(header(CabecerasDeNegocio.NEGOCIO, negocioB.toString()))
                .andExpect(header(CabecerasDeNegocio.USUARIO, admin.toString()))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        // Inventario filtra por negocio: los productos de A no existen para B.
        assertThat(como(adminDe(negocioB), () -> costos.costoUnitarioDe(negocioB, List.of(carne))))
                .isEmpty();
        inventario.verify();
    }

    @Test
    @DisplayName("Criterio 10: si inventario no responde, el costo no se recalcula en cero: 503")
    void inventarioCaido() {
        inventario.expect(requestTo(Matchers.startsWith(INVENTARIO + "/api/inventario/productos/costos")))
                .andRespond(withServerError());

        assertThatThrownBy(() -> como(adminDe(negocioA),
                () -> costos.costoUnitarioDe(negocioA, List.of(carne))))
                .isInstanceOf(ServicioNoDisponibleException.class)
                .hasMessageContaining("inventario");
    }

    @Test
    @DisplayName("Sin permiso de ver productos, inventario responde 403 y no se sigue")
    void sinPermiso() {
        inventario.expect(requestTo(Matchers.startsWith(INVENTARIO + "/api/inventario/productos/costos")))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> como(adminDe(negocioA),
                () -> costos.costoUnitarioDe(negocioA, List.of(carne))))
                .isInstanceOf(SinPermisoException.class);
    }
}
