package com.regenta.reservas.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.regenta.comun.negocio.CabecerasDeNegocio;
import com.regenta.reservas.BaseDeReservas;

/**
 * HU-138. El contrato HTTP que consume servicio-recursos: la ruta, los
 * parámetros y la forma de la respuesta. Lo que filtra ya está probado contra
 * la base en {@code ReservasDeUnRecursoTest}.
 */
@AutoConfigureMockMvc
class ReservasDeUnRecursoApiTest extends BaseDeReservas {

    private static final OffsetDateTime AHORA = OffsetDateTime.now(ZoneOffset.UTC)
            .truncatedTo(ChronoUnit.SECONDS);

    @Autowired
    private MockMvc mvc;

    private final UUID negocio = UUID.randomUUID();
    private final UUID recurso = UUID.randomUUID();

    private MockHttpServletRequestBuilder comoRecursos(MockHttpServletRequestBuilder peticion,
            String permisos) {
        return peticion.header(CabecerasDeNegocio.NEGOCIO, negocio.toString())
                .header(CabecerasDeNegocio.USUARIO, UUID.randomUUID().toString())
                .header(CabecerasDeNegocio.PLAN, "BASICO")
                .header(CabecerasDeNegocio.PATRON, "RESERVA")
                .header(CabecerasDeNegocio.ROLES, "ADMINISTRADOR")
                .header(CabecerasDeNegocio.MODULOS, "RECURSOS,RESERVAS")
                .header(CabecerasDeNegocio.PERMISOS, permisos);
    }

    @Test
    @DisplayName("GET /api/reservas/de-recurso/{id} devuelve las reservas con número, estado y periodo")
    void contrato() throws Exception {
        ejecutarComoElServicio(negocio, "INSERT INTO reservas "
                + "(id, negocio_id, numero, tipo_recurso_id, recurso_id, periodo, estado) VALUES ('"
                + UUID.randomUUID() + "', '" + negocio + "', 'R-API-1', '" + UUID.randomUUID()
                + "', '" + recurso + "', tstzrange('" + AHORA.plusDays(2) + "', '"
                + AHORA.plusDays(4) + "', '[)'), 'CONFIRMADA')");

        mvc.perform(comoRecursos(get("/api/reservas/de-recurso/{recursoId}", recurso)
                .param("desde", AHORA.plusDays(1).toString())
                .param("hasta", AHORA.plusDays(3).toString())
                .param("soloConfirmadas", "true"), "RESERVAS_RESERVA_VER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").isNotEmpty())
                .andExpect(jsonPath("$[0].numero").value("R-API-1"))
                .andExpect(jsonPath("$[0].estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$[0].desde").isNotEmpty())
                .andExpect(jsonPath("$[0].hasta").isNotEmpty());
    }

    @Test
    @DisplayName("Sin hasta ni soloConfirmadas, trae las que ocupan el recurso de ahí en adelante")
    void sinHasta() throws Exception {
        mvc.perform(comoRecursos(get("/api/reservas/de-recurso/{recursoId}", UUID.randomUUID())
                .param("desde", AHORA.toString()), "RESERVAS_RESERVA_VER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Sin el permiso de ver reservas responde 403")
    void sinPermiso() throws Exception {
        mvc.perform(comoRecursos(get("/api/reservas/de-recurso/{recursoId}", recurso)
                .param("desde", AHORA.toString()), "RECURSOS_RECURSO_VER"))
                .andExpect(status().isForbidden());
    }
}
