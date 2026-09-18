package com.regenta.recursos.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

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
 * El catálogo de recursos tiene que salir al bus (HU-099). Reportes necesita
 * saber cuántas habitaciones existen y cuáles están fuera de servicio: sin ese
 * denominador no hay ocupación ni RevPAR que calcular, y ningún evento del
 * patrón Reserva lo traía.
 *
 * <p>Es catálogo, no transacción: se publica el estado completo del recurso en
 * cada cambio, no un delta. Quien lo consume hace un upsert y no tiene que
 * haber visto los eventos anteriores para tener la foto correcta.
 */
class EventosDelCatalogoDeRecursosTest extends BaseDeRecursos {

    private static final Set<String> ADMIN = Set.of("RECURSOS_RECURSO_VER", "RECURSOS_RECURSO_CREAR",
            "RECURSOS_RECURSO_EDITAR", "RECURSOS_RECURSO_ELIMINAR");

    @Autowired
    private GestionDeRecursos recursos;
    @Autowired
    private GestionDeTiposDeRecurso tipos;
    @Autowired
    private ConsultaDeReservasDeRecursoStub reservas;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();

    @BeforeEach
    void limpiar() {
        reservas.reiniciar();
    }

    private UUID tipo(UUID negocio, String nombre) {
        return enContexto(negocio, admin, ADMIN, () -> tipos.crear(new SolicitudDeTipoDeRecurso(
                nombre, null, "NOCHE", 1440, 1440, 2, false, 0, 0))).id();
    }

    private RecursoDelNegocio crear(UUID negocio, UUID tipoId, String codigo) {
        return enContexto(negocio, admin, ADMIN, () -> recursos.crear(new SolicitudDeRecurso(
                tipoId, codigo, "Habitación " + codigo, null, null, 2, "1", "Norte", Map.of(), null)));
    }

    private List<String> payloads(UUID recursoId, String tipoEvento) {
        return consultar("SELECT payload FROM outbox_eventos WHERE agregado_id = '" + recursoId
                + "' AND tipo_evento = '" + tipoEvento + "' ORDER BY id");
    }

    @Test
    @DisplayName("Crear un recurso publica recurso_creado con su tipo, su estado y su capacidad")
    void crearPublicaElRecursoCompleto() {
        UUID tipoId = tipo(negocioA, "Habitación doble");

        RecursoDelNegocio recurso = crear(negocioA, tipoId, "101");

        List<String> eventos = payloads(recurso.id(), "recurso_creado");
        assertThat(eventos).hasSize(1);
        assertThat(eventos.get(0))
                .contains("\"negocio_id\": \"" + negocioA + "\"")
                .contains("\"recurso_id\": \"" + recurso.id() + "\"")
                .contains("\"tipo_recurso_id\": \"" + tipoId + "\"")
                .contains("\"tipo_recurso_nombre\": \"Habitación doble\"")
                .contains("\"codigo\": \"101\"")
                .contains("\"capacidad\": 2")
                .contains("\"estado\": \"DISPONIBLE\"")
                .contains("\"activo\": true");
    }

    @Test
    @DisplayName("Sacar un recurso de servicio publica recurso_actualizado con el estado nuevo")
    void cambiarEstadoPublicaElEstadoNuevo() {
        UUID tipoId = tipo(negocioA, "Habitación");
        RecursoDelNegocio recurso = crear(negocioA, tipoId, "102");

        enContexto(negocioA, admin, ADMIN,
                () -> recursos.cambiarEstado(recurso.id(), "MANTENIMIENTO"));

        List<String> eventos = payloads(recurso.id(), "recurso_actualizado");
        assertThat(eventos).hasSize(1);
        assertThat(eventos.get(0))
                .contains("\"estado\": \"MANTENIMIENTO\"")
                .contains("\"activo\": true");
    }

    @Test
    @DisplayName("Editar un recurso publica recurso_actualizado con lo que quedó")
    void actualizarPublicaLoQueQuedo() {
        UUID tipoId = tipo(negocioA, "Habitación");
        RecursoDelNegocio recurso = crear(negocioA, tipoId, "103");

        enContexto(negocioA, admin, ADMIN, () -> recursos.actualizar(recurso.id(),
                new SolicitudDeRecurso(tipoId, "103", "Suite 103", null, null, 4, "1", "Sur",
                        Map.of(), null)));

        List<String> eventos = payloads(recurso.id(), "recurso_actualizado");
        assertThat(eventos).hasSize(1);
        assertThat(eventos.get(0))
                .contains("\"nombre\": \"Suite 103\"")
                .contains("\"capacidad\": 4");
    }

    @Test
    @DisplayName("Eliminar un recurso publica recurso_eliminado: deja de contar para la ocupación")
    void eliminarPublicaQueYaNoCuenta() {
        UUID tipoId = tipo(negocioA, "Habitación");
        RecursoDelNegocio recurso = crear(negocioA, tipoId, "104");

        enContexto(negocioA, admin, ADMIN, () -> recursos.eliminar(recurso.id()));

        List<String> eventos = payloads(recurso.id(), "recurso_eliminado");
        assertThat(eventos).hasSize(1);
        assertThat(eventos.get(0))
                .contains("\"recurso_id\": \"" + recurso.id() + "\"")
                .contains("\"activo\": false");
    }

    @Test
    @DisplayName("Un recurso con reservas futuras no se elimina, y entonces tampoco publica nada")
    void loQueNoSeEliminaNoSePublica() {
        UUID tipoId = tipo(negocioA, "Habitación");
        RecursoDelNegocio recurso = crear(negocioA, tipoId, "105");
        reservas.conReservasFuturas(recurso.id());

        try {
            enContexto(negocioA, admin, ADMIN, () -> recursos.eliminar(recurso.id()));
        } catch (RuntimeException esperada) {
            // El criterio 3 de HU-065 ya prueba el rechazo; aquí importa el bus.
        }

        assertThat(payloads(recurso.id(), "recurso_eliminado")).isEmpty();
    }

    @Test
    @DisplayName("Cada evento se registra a nombre de su negocio, no del otro")
    void cadaEventoEsDeSuNegocio() {
        UUID tipoA = tipo(negocioA, "Habitación");
        UUID tipoB = tipo(negocioB, "Cancha");
        RecursoDelNegocio deA = crear(negocioA, tipoA, "201");
        RecursoDelNegocio deB = crear(negocioB, tipoB, "201");

        assertThat(consultar("SELECT count(*) FROM outbox_eventos WHERE negocio_id = '" + negocioA
                + "' AND agregado_id = '" + deB.id() + "'")).containsExactly("0");
        assertThat(payloads(deA.id(), "recurso_creado").get(0))
                .contains("\"negocio_id\": \"" + negocioA + "\"");
        assertThat(payloads(deB.id(), "recurso_creado").get(0))
                .contains("\"negocio_id\": \"" + negocioB + "\"");
    }
}
