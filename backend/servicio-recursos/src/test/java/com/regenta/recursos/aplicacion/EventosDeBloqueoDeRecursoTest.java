package com.regenta.recursos.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.recursos.BaseDeRecursos;
import com.regenta.recursos.infra.ConsultaDeReservasDeRecursoStub;

/**
 * HU-136 criterio 1. Un bloqueo por fechas no llegaba al bus, así que para
 * Reportes la habitación en obra seguía contando en el denominador de la
 * ocupación y el hotel salía peor de lo que fue.
 */
class EventosDeBloqueoDeRecursoTest extends BaseDeRecursos {

    private static final Set<String> ADMIN = Set.of("RECURSOS_RECURSO_VER",
            "RECURSOS_RECURSO_CREAR", "RECURSOS_RECURSO_EDITAR");

    private static final OffsetDateTime MAR_1 = OffsetDateTime.parse("2026-03-01T00:00:00Z");
    private static final OffsetDateTime MAR_5 = OffsetDateTime.parse("2026-03-05T00:00:00Z");

    @Autowired
    private GestionDeBloqueos bloqueos;
    @Autowired
    private GestionDeTiposDeRecurso tipos;
    @Autowired
    private GestionDeRecursos recursos;
    @Autowired
    private ConsultaDeReservasDeRecursoStub reservas;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();

    private UUID tipoDelUltimoRecurso;

    @BeforeEach
    void limpiar() {
        reservas.reiniciar();
    }

    private UUID recurso(UUID negocio, String codigo) {
        UUID tipo = enContexto(negocio, admin, ADMIN, () -> tipos.crear(new SolicitudDeTipoDeRecurso(
                "Habitación " + codigo, null, "NOCHE", 1440, 1440, 2, false, 0, 0))).id();
        tipoDelUltimoRecurso = tipo;
        return enContexto(negocio, admin, ADMIN, () -> recursos.crear(new SolicitudDeRecurso(
                tipo, codigo, "Recurso " + codigo, null, null, 2, "1", "Norte", Map.of(), null)))
                .id();
    }

    private List<String> payloads(UUID agregadoId, String tipoEvento) {
        return consultar("SELECT payload FROM outbox_eventos WHERE agregado_id = '" + agregadoId
                + "' AND tipo_evento = '" + tipoEvento + "' ORDER BY id");
    }

    @Test
    @DisplayName("Criterio 1: crear un bloqueo publica bloqueo_recurso_creado con periodo y motivo")
    void crearPublicaElBloqueo() {
        UUID r = recurso(negocioA, "101");
        UUID tipo = tipoDelUltimoRecurso;

        BloqueoCreado creado = enContexto(negocioA, admin, ADMIN, () -> bloqueos.crear(r,
                new SolicitudDeBloqueo(MAR_1, MAR_5, "MANTENIMIENTO", "Pintura")));

        List<String> eventos = payloads(creado.bloqueo().id(), "bloqueo_recurso_creado");
        assertThat(eventos).hasSize(1);
        assertThat(eventos.get(0))
                .contains("\"negocio_id\": \"" + negocioA + "\"")
                .contains("\"recurso_id\": \"" + r + "\"")
                .contains("\"tipo_recurso_id\": \"" + tipo + "\"")
                .contains("\"motivo\": \"MANTENIMIENTO\"")
                .contains("\"detalle\": \"Pintura\"")
                .contains("2026-03-01")
                .contains("2026-03-05");
    }

    @Test
    @DisplayName("Criterio 1: levantar un bloqueo publica bloqueo_recurso_levantado")
    void levantarPublicaElBloqueo() {
        UUID r = recurso(negocioA, "102");
        BloqueoCreado creado = enContexto(negocioA, admin, ADMIN, () -> bloqueos.crear(r,
                new SolicitudDeBloqueo(MAR_1, MAR_5, "MANTENIMIENTO", "Pintura")));

        enContexto(negocioA, admin, ADMIN, () -> bloqueos.eliminar(creado.bloqueo().id()));

        List<String> eventos = payloads(creado.bloqueo().id(), "bloqueo_recurso_levantado");
        assertThat(eventos).hasSize(1);
        assertThat(eventos.get(0))
                .contains("\"recurso_id\": \"" + r + "\"")
                .contains("\"bloqueo_id\": \"" + creado.bloqueo().id() + "\"");
    }

    @Test
    @DisplayName("El bloqueo del otro negocio no aparece en el outbox de este")
    void aislamientoEntreNegocios() {
        UUID enB = recurso(negocioB, "201");
        BloqueoCreado delB = enContexto(negocioB, admin, ADMIN, () -> bloqueos.crear(enB,
                new SolicitudDeBloqueo(MAR_1, MAR_5, "MANTENIMIENTO", "Obra")));

        assertThat(consultar("SELECT negocio_id FROM outbox_eventos WHERE agregado_id = '"
                + delB.bloqueo().id() + "' AND tipo_evento = 'bloqueo_recurso_creado'"))
                .containsExactly(negocioB.toString());
    }
}
