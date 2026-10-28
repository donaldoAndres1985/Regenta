package com.regenta.comandas.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
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
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.regenta.comandas.aplicacion.CatalogoDeMenu;
import com.regenta.comandas.aplicacion.CatalogoDeMenu.Cotizacion;
import com.regenta.comandas.aplicacion.CatalogoDeMenu.ItemDeMenu;
import com.regenta.comun.errores.ErrorDeAplicacion;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.negocio.CabecerasDeNegocio;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.DatosDelNegocio;

/**
 * HU-138. servicio-comandas le pregunta a servicio-menu por el ítem y por la
 * selección de modificadores, en vez de un stub. servicio-menu no se levanta:
 * se simula su contrato HTTP (HU-077 y HU-078) con dos negocios.
 */
class CatalogoDeMenuHttpTest {

    private static final String MENU = "http://servicio-menu";

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID mesero = UUID.randomUUID();
    private final UUID bandeja = UUID.randomUUID();
    private final UUID estacion = UUID.randomUUID();

    private MockRestServiceServer menu;
    private CatalogoDeMenu catalogo;

    @BeforeEach
    void menuSimulado() {
        RestClient.Builder builder = RestClient.builder();
        menu = MockRestServiceServer.bindTo(builder).build();
        catalogo = new CatalogoDeMenuHttp(builder, MENU);
    }

    private <T> T comoMeseroDe(UUID negocio, Supplier<T> tarea) {
        AtomicReference<T> resultado = new AtomicReference<>();
        ContextoDeNegocio.en(new DatosDelNegocio(negocio, mesero, "BASICO", "COMANDA",
                Set.of("MESERO"), Set.of("COMANDAS", "MENU"),
                Set.of("COMANDAS_COMANDA_EDITAR", "MENU_PLATO_VER"), Set.of()),
                () -> resultado.set(tarea.get()));
        return resultado.get();
    }

    private String itemJson(boolean disponible, boolean activo) {
        return """
                {"id":"%s","categoriaMenuId":"%s","estacionId":"%s","codigo":"BP-01",
                 "nombre":"Bandeja paisa","descripcion":null,"tipo":"PLATO","precio":32000.00,
                 "precioIncluyeImpuesto":true,"costoEstimado":12000.00,"tiempoPreparacionMin":20,
                 "curso":"FUERTE","disponible":%s,"atributos":{},"imagenUrl":null,"orden":1,
                 "activo":%s}
                """.formatted(bandeja, UUID.randomUUID(), estacion, disponible, activo);
    }

    @Test
    @DisplayName("Criterio 1: la línea toma nombre, precio, estación, curso y costo reales del menú")
    void itemReal() {
        menu.expect(requestTo(MENU + "/api/menu/items/" + bandeja))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(itemJson(true, true), MediaType.APPLICATION_JSON));

        ItemDeMenu item = comoMeseroDe(negocioA, () -> catalogo.item(negocioA, bandeja));

        assertThat(item.id()).isEqualTo(bandeja);
        assertThat(item.nombre()).isEqualTo("Bandeja paisa");
        assertThat(item.precio()).isEqualByComparingTo("32000");
        assertThat(item.estacionId()).isEqualTo(estacion);
        assertThat(item.curso()).isEqualTo("FUERTE");
        assertThat(item.costoEstimado()).isEqualByComparingTo("12000");
        assertThat(item.disponible()).isTrue();
        menu.verify();
    }

    @Test
    @DisplayName("Criterio 1: un ítem marcado agotado en el menú llega como no disponible")
    void itemAgotado() {
        menu.expect(requestTo(MENU + "/api/menu/items/" + bandeja))
                .andRespond(withSuccess(itemJson(false, true), MediaType.APPLICATION_JSON));

        ItemDeMenu item = comoMeseroDe(negocioA, () -> catalogo.item(negocioA, bandeja));

        assertThat(item.disponible()).isFalse();
    }

    @Test
    @DisplayName("Criterio 1: un ítem que el menú no tiene, o que está inactivo, no existe para la comanda")
    void itemInexistenteOInactivo() {
        UUID otro = UUID.randomUUID();
        menu.expect(requestTo(MENU + "/api/menu/items/" + otro)).andRespond(withResourceNotFound());
        menu.expect(requestTo(MENU + "/api/menu/items/" + bandeja))
                .andRespond(withSuccess(itemJson(true, false), MediaType.APPLICATION_JSON));

        assertThat(comoMeseroDe(negocioA, () -> catalogo.item(negocioA, otro))).isNull();
        assertThat(comoMeseroDe(negocioA, () -> catalogo.item(negocioA, bandeja))).isNull();
    }

    @Test
    @DisplayName("Criterio 2: la selección de modificadores se valida contra los grupos reales del menú")
    void cotizaContraElMenu() {
        UUID sinCebolla = UUID.randomUUID();
        UUID huevo = UUID.randomUUID();
        menu.expect(requestTo(MENU + "/api/menu/items/" + bandeja + "/seleccion-modificadores"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("[\"%s\",\"%s\"]".formatted(sinCebolla, huevo)))
                .andRespond(withSuccess("""
                        {"elegidos":[
                          {"id":"%s","grupoId":"%s","grupoNombre":"Quitar","nombre":"Sin cebolla","precioExtra":0},
                          {"id":"%s","grupoId":"%s","grupoNombre":"Adicionales","nombre":"Huevo","precioExtra":2500.00}
                        ],"extraTotal":2500.00}
                        """.formatted(sinCebolla, UUID.randomUUID(), huevo, UUID.randomUUID()),
                        MediaType.APPLICATION_JSON));

        Cotizacion cot = comoMeseroDe(negocioA,
                () -> catalogo.cotizarModificadores(negocioA, bandeja, List.of(sinCebolla, huevo)));

        assertThat(cot.extraPorUnidad()).isEqualByComparingTo("2500");
        assertThat(cot.elegidos()).extracting(CatalogoDeMenu.ModificadorElegido::nombre)
                .containsExactly("Sin cebolla", "Huevo");
        assertThat(cot.elegidos().get(1).id()).isEqualTo(huevo);
        assertThat(cot.elegidos().get(1).precioExtra()).isEqualByComparingTo("2500");
        menu.verify();
    }

    @Test
    @DisplayName("Criterio 2: sin modificadores elegidos, el menú igual valida los grupos obligatorios")
    void sinModificadoresTambienSeValida() {
        menu.expect(requestTo(MENU + "/api/menu/items/" + bandeja + "/seleccion-modificadores"))
                .andExpect(content().json("[]"))
                .andRespond(withSuccess("{\"elegidos\":[],\"extraTotal\":0}",
                        MediaType.APPLICATION_JSON));

        Cotizacion cot = comoMeseroDe(negocioA,
                () -> catalogo.cotizarModificadores(negocioA, bandeja, null));

        assertThat(cot.elegidos()).isEmpty();
        assertThat(cot.extraPorUnidad()).isEqualByComparingTo("0");
        menu.verify();
    }

    @Test
    @DisplayName("Criterio 2: si el menú rechaza la selección (422), la línea se rechaza con su mismo motivo")
    void seleccionRechazada() {
        menu.expect(requestTo(MENU + "/api/menu/items/" + bandeja + "/seleccion-modificadores"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("""
                                {"title":"Regla de negocio","status":422,
                                 "detail":"Falta elegir en el grupo obligatorio \\"Término\\""}
                                """));

        assertThatThrownBy(() -> comoMeseroDe(negocioA,
                () -> catalogo.cotizarModificadores(negocioA, bandeja, List.of())))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessage("Falta elegir en el grupo obligatorio \"Término\"");
    }

    @Test
    @DisplayName("Criterio 2: un modificador que el menú no conoce (404) es un no encontrado")
    void modificadorInexistente() {
        menu.expect(requestTo(MENU + "/api/menu/items/" + bandeja + "/seleccion-modificadores"))
                .andRespond(withResourceNotFound());

        assertThatThrownBy(() -> comoMeseroDe(negocioA, () -> catalogo.cotizarModificadores(
                negocioA, bandeja, List.of(UUID.randomUUID()))))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    @DisplayName("Criterio 9: la consulta viaja con el negocio y la identidad de quien la hace, nunca con otro")
    void viajaConElNegocioDeQuienPregunta() {
        menu.expect(requestTo(MENU + "/api/menu/items/" + bandeja))
                .andExpect(header(CabecerasDeNegocio.NEGOCIO, negocioA.toString()))
                .andExpect(header(CabecerasDeNegocio.USUARIO, mesero.toString()))
                .andExpect(header(CabecerasDeNegocio.PATRON, "COMANDA"))
                // Con los permisos de quien pregunta: servicio-menu exige MENU_PLATO_VER.
                .andExpect(peticion -> assertThat(
                        peticion.getHeaders().getFirst(CabecerasDeNegocio.PERMISOS))
                        .contains("MENU_PLATO_VER"))
                .andRespond(withSuccess(itemJson(true, true), MediaType.APPLICATION_JSON));
        menu.expect(requestTo(MENU + "/api/menu/items/" + bandeja))
                .andExpect(header(CabecerasDeNegocio.NEGOCIO, negocioB.toString()))
                .andRespond(withResourceNotFound());

        assertThat(comoMeseroDe(negocioA, () -> catalogo.item(negocioA, bandeja))).isNotNull();
        // El mismo ítem, pedido desde el negocio B: el menú filtra por negocio y responde 404.
        assertThat(comoMeseroDe(negocioB, () -> catalogo.item(negocioB, bandeja))).isNull();
        menu.verify();
    }

    @Test
    @DisplayName("Criterio 10: si el menú no responde, la operación falla con un error claro, no con un vacío")
    void menuCaido() {
        menu.expect(requestTo(MENU + "/api/menu/items/" + bandeja)).andRespond(withServerError());
        menu.expect(requestTo(MENU + "/api/menu/items/" + bandeja + "/seleccion-modificadores"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> comoMeseroDe(negocioA, () -> catalogo.item(negocioA, bandeja)))
                .isInstanceOf(ErrorDeAplicacion.class)
                .satisfies(e -> assertThat(((ErrorDeAplicacion) e).getEstado())
                        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE))
                .hasMessageContaining("menú");
        assertThatThrownBy(() -> comoMeseroDe(negocioA,
                () -> catalogo.cotizarModificadores(negocioA, bandeja, List.of())))
                .isInstanceOf(ErrorDeAplicacion.class)
                .satisfies(e -> assertThat(((ErrorDeAplicacion) e).getEstado())
                        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }
}
