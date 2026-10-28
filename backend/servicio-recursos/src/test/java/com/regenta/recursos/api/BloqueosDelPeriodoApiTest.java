package com.regenta.recursos.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.regenta.comun.negocio.CabecerasDeNegocio;
import com.regenta.recursos.BaseDeRecursos;
import com.regenta.recursos.aplicacion.GestionDeBloqueos;
import com.regenta.recursos.aplicacion.GestionDeRecursos;
import com.regenta.recursos.aplicacion.GestionDeTiposDeRecurso;
import com.regenta.recursos.aplicacion.SolicitudDeBloqueo;
import com.regenta.recursos.aplicacion.SolicitudDeRecurso;
import com.regenta.recursos.aplicacion.SolicitudDeTipoDeRecurso;

/**
 * HU-138. El contrato que consume servicio-reservas: la ruta, los parámetros y
 * la forma de la respuesta. Lo que filtra está probado contra la base en
 * {@code BloqueosDelPeriodoTest}.
 */
@AutoConfigureMockMvc
class BloqueosDelPeriodoApiTest extends BaseDeRecursos {

    private static final Set<String> ADMIN = Set.of("RECURSOS_RECURSO_VER",
            "RECURSOS_RECURSO_CREAR", "RECURSOS_RECURSO_EDITAR");

    @Autowired
    private MockMvc mvc;
    @Autowired
    private GestionDeTiposDeRecurso tipos;
    @Autowired
    private GestionDeRecursos recursos;
    @Autowired
    private GestionDeBloqueos bloqueos;

    private final UUID negocio = UUID.randomUUID();

    private MockHttpServletRequestBuilder comoReservas(MockHttpServletRequestBuilder peticion,
            String permisos) {
        return peticion.header(CabecerasDeNegocio.NEGOCIO, negocio.toString())
                .header(CabecerasDeNegocio.USUARIO, UUID.randomUUID().toString())
                .header(CabecerasDeNegocio.PLAN, "BASICO")
                .header(CabecerasDeNegocio.PATRON, "RESERVA")
                .header(CabecerasDeNegocio.ROLES, "RECEPCIONISTA")
                .header(CabecerasDeNegocio.MODULOS, "RECURSOS,RESERVAS")
                .header(CabecerasDeNegocio.PERMISOS, permisos);
    }

    @Test
    @DisplayName("GET /api/recursos/bloqueos devuelve los bloqueos del periodo con recurso, tramo y motivo")
    void contrato() throws Exception {
        UUID admin = UUID.randomUUID();
        UUID tipo = enContexto(negocio, admin, ADMIN, () -> tipos.crear(new SolicitudDeTipoDeRecurso(
                "Habitación", null, "NOCHE", 1440, 1440, 2, false, 0, 0))).id();
        UUID recurso = enContexto(negocio, admin, ADMIN, () -> recursos.crear(new SolicitudDeRecurso(
                tipo, "API-1", "Recurso API", null, null, 2, "1", null, Map.of(), null))).id();
        enContexto(negocio, admin, ADMIN, () -> bloqueos.crear(recurso, new SolicitudDeBloqueo(
                OffsetDateTime.parse("2026-05-02T00:00:00Z"),
                OffsetDateTime.parse("2026-05-04T00:00:00Z"), "MANTENIMIENTO", null)));

        mvc.perform(comoReservas(get("/api/recursos/bloqueos")
                .param("desde", "2026-05-01T00:00:00Z")
                .param("hasta", "2026-05-03T00:00:00Z"), "RECURSOS_RECURSO_VER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].recursoId").value(recurso.toString()))
                .andExpect(jsonPath("$[0].desde").isNotEmpty())
                .andExpect(jsonPath("$[0].hasta").isNotEmpty())
                .andExpect(jsonPath("$[0].motivo").value("MANTENIMIENTO"));
    }

    @Test
    @DisplayName("Sin el permiso de ver recursos responde 403")
    void sinPermiso() throws Exception {
        mvc.perform(comoReservas(get("/api/recursos/bloqueos")
                .param("desde", "2026-05-01T00:00:00Z")
                .param("hasta", "2026-05-03T00:00:00Z"), "RESERVAS_RESERVA_VER"))
                .andExpect(status().isForbidden());
    }
}
