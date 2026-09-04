package com.regenta.comun.negocio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

/** El contexto sale de las cabeceras del gateway, y se limpia pase lo que pase. */
class FiltroDeNegocioTest {

    private static final UUID NEGOCIO = UUID.fromString("9f1c6b0e-3a2d-4c58-9d21-5b7a0e4f1c33");
    private static final UUID USUARIO = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID SUCURSAL = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    private final FiltroDeNegocio filtro = new FiltroDeNegocio();

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    @Test
    @DisplayName("Arma el contexto con lo que escribio el gateway")
    void armaElContexto() throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest("GET", "/api/usuarios");
        peticion.addHeader(CabecerasDeNegocio.NEGOCIO, NEGOCIO.toString());
        peticion.addHeader(CabecerasDeNegocio.USUARIO, USUARIO.toString());
        peticion.addHeader(CabecerasDeNegocio.PLAN, "PROFESIONAL");
        peticion.addHeader(CabecerasDeNegocio.PATRON, "VENTA_DIRECTA");
        peticion.addHeader(CabecerasDeNegocio.ROLES, "administrador, cajero");
        peticion.addHeader(CabecerasDeNegocio.MODULOS, "VENTAS,FACTURACION");
        peticion.addHeader(CabecerasDeNegocio.PERMISOS, "VENTAS_VENTA_CREAR");
        peticion.addHeader(CabecerasDeNegocio.SUCURSALES, SUCURSAL + ", no-es-un-uuid");

        AtomicReference<DatosDelNegocio> visto = new AtomicReference<>();
        filtro.doFilter(peticion, new MockHttpServletResponse(),
                (req, res) -> visto.set(ContextoDeNegocio.actual()));

        DatosDelNegocio datos = visto.get();
        assertThat(datos.negocio()).isEqualTo(NEGOCIO);
        assertThat(datos.usuario()).isEqualTo(USUARIO);
        assertThat(datos.plan()).isEqualTo("PROFESIONAL");
        assertThat(datos.patron()).isEqualTo("VENTA_DIRECTA");
        assertThat(datos.roles()).containsExactlyInAnyOrder("ADMINISTRADOR", "CAJERO");
        assertThat(datos.tieneModulo("facturacion")).isTrue();
        assertThat(datos.tieneModulo("INVENTARIO")).isFalse();
        assertThat(datos.puede("ventas_venta_crear")).isTrue();
        assertThat(datos.puede("VENTAS_PRECIO_EDITAR")).isFalse();
        assertThat(datos.sucursales()).containsExactly(SUCURSAL);
        assertThat(datos.alcanza(SUCURSAL)).isTrue();
        assertThat(datos.alcanza(UUID.randomUUID())).isFalse();

        assertThat(ContextoDeNegocio.hay())
                .as("el hilo vuelve al pool sin contexto")
                .isFalse();
    }

    @Test
    @DisplayName("Sin negocio no se inventa uno: la peticion sigue sin contexto")
    void sinNegocioNoHayContexto() throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest("GET", "/api/usuarios");
        peticion.addHeader(CabecerasDeNegocio.PLAN, "BASICO");

        AtomicReference<Boolean> habia = new AtomicReference<>();
        filtro.doFilter(peticion, new MockHttpServletResponse(),
                (req, res) -> habia.set(ContextoDeNegocio.hay()));

        assertThat(habia.get()).isFalse();
    }

    @Test
    @DisplayName("Un negocio que no es UUID se ignora: no se opera con basura")
    void negocioInvalidoSeIgnora() throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest("GET", "/api/usuarios");
        peticion.addHeader(CabecerasDeNegocio.NEGOCIO, "'; drop table usuarios; --");

        AtomicReference<Boolean> habia = new AtomicReference<>();
        filtro.doFilter(peticion, new MockHttpServletResponse(),
                (req, res) -> habia.set(ContextoDeNegocio.hay()));

        assertThat(habia.get()).isFalse();
    }

    @Test
    @DisplayName("Si la peticion revienta, el contexto igual se limpia")
    void seLimpiaAunqueFalle() {
        MockHttpServletRequest peticion = new MockHttpServletRequest("GET", "/api/usuarios");
        peticion.addHeader(CabecerasDeNegocio.NEGOCIO, NEGOCIO.toString());
        FilterChain cadena = (req, res) -> {
            throw new ServletException("algo exploto");
        };

        assertThatThrownBy(() -> filtro.doFilter(peticion, new MockHttpServletResponse(), cadena))
                .isInstanceOf(ServletException.class);

        assertThat(ContextoDeNegocio.hay()).isFalse();
    }

    @Test
    @DisplayName("Sin contexto, pedir el negocio falla en vez de devolver null")
    void sinContextoFalla() {
        assertThatThrownBy(ContextoDeNegocio::negocioActual)
                .isInstanceOf(SinNegocioException.class);
    }
}
