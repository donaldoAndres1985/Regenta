package com.regenta.usuarios.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.usuarios.BaseDeUsuarios;
import com.regenta.usuarios.aplicacion.SolicitudDeAlta;
import com.regenta.usuarios.domain.Patrones;

/**
 * El alta por HTTP. Lo que se prueba aqui no es el alta —eso ya esta probado
 * contra la base— sino la puerta: quien puede llamarla y que responde cuando el
 * cuerpo no cumple el contrato.
 */
@AutoConfigureMockMvc
class NegociosApiTest extends BaseDeUsuarios {

    private static final String CLAVE_BUENA = "clave-del-operador-de-pruebas";
    private static final AtomicInteger CONSECUTIVO = new AtomicInteger(5000);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private String cuerpo(String password) throws Exception {
        String documento = "8007654" + CONSECUTIVO.incrementAndGet();
        return json.writeValueAsString(new SolicitudDeAlta("Bar La Esquina", "La Esquina SAS",
                "NIT", documento, "3", Patrones.COMANDA, "PROFESIONAL", "CO", "America/Bogota",
                "COP", "es-CO", new SolicitudDeAlta.Administrador("dueno" + documento + "@bar.co",
                        "Luis", "Marin", password)));
    }

    @Test
    @DisplayName("Sin la clave del operador no se da de alta nada")
    void sinClaveNoPasa() throws Exception {
        mvc.perform(post("/api/usuarios/negocios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("clave-de-prueba-larga")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Con una clave que no es, tampoco")
    void conClaveEquivocadaTampoco() throws Exception {
        mvc.perform(post("/api/usuarios/negocios")
                .header(NegociosControlador.CABECERA_OPERADOR, "la-que-yo-me-invente")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("clave-de-prueba-larga")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Con la clave del operador, 201 y el negocio listo")
    void conLaClaveDaDeAlta() throws Exception {
        mvc.perform(post("/api/usuarios/negocios")
                .header(NegociosControlador.CABECERA_OPERADOR, CLAVE_BUENA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("clave-de-prueba-larga")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.negocioId").isNotEmpty())
                .andExpect(jsonPath("$.plan").value("PROFESIONAL"))
                .andExpect(jsonPath("$.patronOperativo").value("COMANDA"))
                .andExpect(jsonPath("$.administradorId").isNotEmpty())
                .andExpect(jsonPath("$.modulosActivos").isArray());
    }

    @Test
    @DisplayName("Una clave de administrador corta no pasa la validacion: 422 con el campo")
    void cuerpoInvalidoResponde422() throws Exception {
        mvc.perform(post("/api/usuarios/negocios")
                .header(NegociosControlador.CABECERA_OPERADOR, CLAVE_BUENA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("corta")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.campos").isNotEmpty());
    }
}
