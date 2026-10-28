package com.regenta.alertas.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

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

import com.regenta.alertas.aplicacion.DirectorioDeUsuarios;
import com.regenta.comun.errores.ServicioNoDisponibleException;
import com.regenta.comun.negocio.CabecerasDeNegocio;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.DatosDelNegocio;

/**
 * HU-138 criterio 8. A quién se entrega una alerta dirigida a un rol (HU-092
 * criterio 5): se le pregunta a servicio-usuarios en vez de a un stub. La
 * alerta la dispara un evento o un barrido, sin usuario: la consulta va como
 * SISTEMA con solo USUARIOS_USUARIO_VER.
 */
class DirectorioDeUsuariosHttpTest {

    private static final String USUARIOS = "http://servicio-usuarios";

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();

    private MockRestServiceServer usuarios;
    private DirectorioDeUsuarios directorio;

    @BeforeEach
    void usuariosSimulado() {
        RestClient.Builder builder = RestClient.builder();
        usuarios = MockRestServiceServer.bindTo(builder).build();
        directorio = new DirectorioDeUsuariosHttp(builder, USUARIOS);
    }

    /** El contexto con que corre el consumidor de eventos de alertas: sin usuario. */
    private <T> T enEventoDe(UUID negocio, Supplier<T> tarea) {
        AtomicReference<T> resultado = new AtomicReference<>();
        ContextoDeNegocio.en(new DatosDelNegocio(negocio, null, "", "VENTA_DIRECTA", Set.of(),
                Set.of("ALERTAS"), Set.of(), Set.of()), () -> resultado.set(tarea.get()));
        return resultado.get();
    }

    @Test
    @DisplayName("Criterio 8: la alerta dirigida a un rol llega a los usuarios reales con ese rol")
    void usuariosReales() {
        UUID ana = UUID.randomUUID();
        UUID luis = UUID.randomUUID();
        usuarios.expect(requestTo(USUARIOS + "/api/usuarios/usuarios/con-rol?rol=GERENTE"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[\"%s\",\"%s\"]".formatted(ana, luis),
                        MediaType.APPLICATION_JSON));

        assertThat(enEventoDe(negocioA, () -> directorio.usuariosConRol(negocioA, "GERENTE")))
                .containsExactly(ana, luis);
        usuarios.verify();
    }

    @Test
    @DisplayName("Criterio 8: sin usuario detrás, la consulta va como SISTEMA con solo el permiso de ver usuarios")
    void comoSistema() {
        usuarios.expect(requestTo(USUARIOS + "/api/usuarios/usuarios/con-rol?rol=GERENTE"))
                .andExpect(header(CabecerasDeNegocio.ROLES, "SISTEMA"))
                .andExpect(header(CabecerasDeNegocio.MODULOS, "USUARIOS"))
                .andExpect(header(CabecerasDeNegocio.PERMISOS, "USUARIOS_USUARIO_VER"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        enEventoDe(negocioA, () -> directorio.usuariosConRol(negocioA, "GERENTE"));
        usuarios.verify();
    }

    @Test
    @DisplayName("Criterio 9: cada negocio consulta sus propios usuarios")
    void viajaConElNegocio() {
        usuarios.expect(requestTo(USUARIOS + "/api/usuarios/usuarios/con-rol?rol=GERENTE"))
                .andExpect(header(CabecerasDeNegocio.NEGOCIO, negocioB.toString()))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(enEventoDe(negocioB, () -> directorio.usuariosConRol(negocioB, "GERENTE")))
                .isEmpty();
        usuarios.verify();
    }

    @Test
    @DisplayName("Criterio 10: si usuarios no responde, la alerta no se da por entregada a nadie: 503")
    void usuariosCaido() {
        usuarios.expect(requestTo(USUARIOS + "/api/usuarios/usuarios/con-rol?rol=GERENTE"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> enEventoDe(negocioA,
                () -> directorio.usuariosConRol(negocioA, "GERENTE")))
                .isInstanceOf(ServicioNoDisponibleException.class)
                .hasMessageContaining("usuarios");
    }
}
