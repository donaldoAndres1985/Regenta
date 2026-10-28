package com.regenta.usuarios.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.regenta.comun.negocio.CabecerasDeNegocio;
import com.regenta.usuarios.BaseDeUsuarios;
import com.regenta.usuarios.aplicacion.AltaDeNegocios;
import com.regenta.usuarios.aplicacion.NegocioCreado;
import com.regenta.usuarios.aplicacion.SolicitudDeAlta;
import com.regenta.usuarios.domain.Patrones;

/**
 * HU-138 criterio 8. El contrato que consume servicio-alertas para saber a quién
 * entregar una alerta dirigida a un rol. Alertas llama con la identidad de
 * SISTEMA: la alerta la dispara un barrido o un evento, no un usuario.
 */
@AutoConfigureMockMvc
class UsuariosConRolApiTest extends BaseDeUsuarios {

    private static final AtomicInteger CONSECUTIVO = new AtomicInteger(8000);

    @Autowired
    private MockMvc mvc;
    @Autowired
    private AltaDeNegocios alta;

    private NegocioCreado negocioNuevo() {
        String documento = "8001234" + CONSECUTIVO.incrementAndGet();
        return alta.registrar(new SolicitudDeAlta("Tienda " + documento, null, "NIT", documento,
                "1", Patrones.VENTA_DIRECTA, "PROFESIONAL", "CO", "America/Bogota", "COP", "es-CO",
                new SolicitudDeAlta.Administrador("jefe" + documento + "@regenta.co", "Ana",
                        "Restrepo", "clave-de-prueba-larga")));
    }

    private MockHttpServletRequestBuilder comoSistema(MockHttpServletRequestBuilder peticion,
            NegocioCreado negocio, String permisos) {
        return peticion.header(CabecerasDeNegocio.NEGOCIO, negocio.negocioId().toString())
                .header(CabecerasDeNegocio.PLAN, "")
                .header(CabecerasDeNegocio.PATRON, "VENTA_DIRECTA")
                .header(CabecerasDeNegocio.ROLES, "SISTEMA")
                .header(CabecerasDeNegocio.MODULOS, "USUARIOS")
                .header(CabecerasDeNegocio.PERMISOS, permisos);
    }

    @Test
    @DisplayName("GET /api/usuarios/usuarios/con-rol devuelve los ids de los usuarios activos con ese rol")
    void contrato() throws Exception {
        NegocioCreado negocio = negocioNuevo();

        mvc.perform(comoSistema(get("/api/usuarios/usuarios/con-rol").param("rol", "ADMINISTRADOR"),
                negocio, "USUARIOS_USUARIO_VER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0]").value(negocio.administradorId().toString()));
    }

    @Test
    @DisplayName("Sin el permiso de ver usuarios responde 403")
    void sinPermiso() throws Exception {
        mvc.perform(comoSistema(get("/api/usuarios/usuarios/con-rol").param("rol", "ADMINISTRADOR"),
                negocioNuevo(), "ALERTAS_ALERTA_VER"))
                .andExpect(status().isForbidden());
    }
}
