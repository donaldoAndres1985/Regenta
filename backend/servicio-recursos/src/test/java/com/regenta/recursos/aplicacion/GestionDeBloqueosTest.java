package com.regenta.recursos.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.recursos.BaseDeRecursos;
import com.regenta.recursos.infra.ConsultaDeReservasDeRecursoStub;

/**
 * HU-067. Bloqueos de recurso por mantenimiento, con dos negocios cargados. El
 * rechazo de solapes lo hace PostgreSQL con el {@code EXCLUDE USING gist}: hay un
 * test que lo prueba contra la base y otro que comprueba el 409 del servicio.
 */
class GestionDeBloqueosTest extends BaseDeRecursos {

    private static final Set<String> ADMIN = Set.of("RECURSOS_RECURSO_VER",
            "RECURSOS_RECURSO_CREAR", "RECURSOS_RECURSO_EDITAR");

    private static final OffsetDateTime MAR_1 = OffsetDateTime.parse("2026-03-01T00:00:00Z");
    private static final OffsetDateTime MAR_5 = OffsetDateTime.parse("2026-03-05T00:00:00Z");
    private static final OffsetDateTime MAR_10 = OffsetDateTime.parse("2026-03-10T00:00:00Z");
    private static final OffsetDateTime MAR_12 = OffsetDateTime.parse("2026-03-12T00:00:00Z");

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

    @BeforeEach
    void limpiar() {
        reservas.reiniciar();
    }

    private UUID recurso(UUID negocio, String codigo) {
        UUID tipo = enContexto(negocio, admin, ADMIN, () -> tipos.crear(new SolicitudDeTipoDeRecurso(
                "Habitación " + codigo, null, "NOCHE", 1440, 1440, 2, false, 0, 0))).id();
        return enContexto(negocio, admin, ADMIN, () -> recursos.crear(new SolicitudDeRecurso(
                tipo, codigo, "Recurso " + codigo, null, null, 2, "1", "Norte", Map.of(), null)))
                .id();
    }

    private SolicitudDeBloqueo entre(OffsetDateTime desde, OffsetDateTime hasta) {
        return new SolicitudDeBloqueo(desde, hasta, "MANTENIMIENTO", "Pintura");
    }

    @Test
    @DisplayName("Criterio 1: con el bloqueo puesto, el recurso no está disponible en ese periodo")
    void bloqueoSacaDeDisponible() {
        UUID r = recurso(negocioA, "101");
        enContexto(negocioA, admin, ADMIN, () -> bloqueos.crear(r, entre(MAR_1, MAR_5)));

        DisponibilidadDeRecurso enObra = enContexto(negocioA, admin, ADMIN,
                () -> bloqueos.disponibilidad(r, OffsetDateTime.parse("2026-03-02T00:00:00Z"),
                        OffsetDateTime.parse("2026-03-03T00:00:00Z")));
        assertThat(enObra.disponible()).isFalse();
        assertThat(enObra.bloqueos()).hasSize(1);

        DisponibilidadDeRecurso despues = enContexto(negocioA, admin, ADMIN,
                () -> bloqueos.disponibilidad(r, MAR_10, MAR_12));
        assertThat(despues.disponible()).isTrue();
        assertThat(despues.bloqueos()).isEmpty();

        // El periodo [MAR_5, ...) solo toca el final del bloqueo [MAR_1, MAR_5): no se solapan.
        DisponibilidadDeRecurso pegado = enContexto(negocioA, admin, ADMIN,
                () -> bloqueos.disponibilidad(r, MAR_5, OffsetDateTime.parse("2026-03-07T00:00:00Z")));
        assertThat(pegado.disponible()).isTrue();
    }

    @Test
    @DisplayName("Criterio 2: PostgreSQL rechaza por el EXCLUDE un segundo bloqueo que se solapa")
    void postgresRechazaElSolape() {
        UUID r = recurso(negocioA, "102");
        UUID primero = UUID.randomUUID();
        UUID segundo = UUID.randomUUID();
        UUID tercero = UUID.randomUUID();

        ejecutarComoElServicio(negocioA, "INSERT INTO bloqueos_recurso "
                + "(id, negocio_id, recurso_id, periodo, motivo) VALUES ('" + primero + "', '"
                + negocioA + "', '" + r + "', tstzrange('2026-03-01 00:00+00','2026-03-05 00:00+00','[)'),"
                + " 'MANTENIMIENTO')");

        assertThatThrownBy(() -> ejecutarComoElServicio(negocioA, "INSERT INTO bloqueos_recurso "
                + "(id, negocio_id, recurso_id, periodo, motivo) VALUES ('" + segundo + "', '"
                + negocioA + "', '" + r + "', tstzrange('2026-03-03 00:00+00','2026-03-08 00:00+00','[)'),"
                + " 'EVENTO')"))
                .isInstanceOf(IllegalStateException.class);

        // Uno pegado al final del primero, sin solapar, sí entra.
        assertThatCode(() -> ejecutarComoElServicio(negocioA, "INSERT INTO bloqueos_recurso "
                + "(id, negocio_id, recurso_id, periodo, motivo) VALUES ('" + tercero + "', '"
                + negocioA + "', '" + r + "', tstzrange('2026-03-05 00:00+00','2026-03-08 00:00+00','[)'),"
                + " 'LIMPIEZA')"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Criterio 2: el servicio traduce el solape a un 409")
    void servicioDevuelve409EnSolape() {
        UUID r = recurso(negocioA, "103");
        enContexto(negocioA, admin, ADMIN, () -> bloqueos.crear(r, entre(MAR_1, MAR_5)));

        assertThatThrownBy(() -> enContexto(negocioA, admin, ADMIN, () -> bloqueos.crear(r,
                entre(OffsetDateTime.parse("2026-03-03T00:00:00Z"),
                        OffsetDateTime.parse("2026-03-09T00:00:00Z")))))
                .isInstanceOf(ConflictoDeEstadoException.class);

        // Un bloqueo adyacente (empieza donde termina el otro) se permite.
        assertThatCode(() -> enContexto(negocioA, admin, ADMIN, () -> bloqueos.crear(r,
                entre(MAR_5, OffsetDateTime.parse("2026-03-07T00:00:00Z")))))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Criterio 3: un bloqueo sobre reservas confirmadas se crea igual, pero las lista")
    void bloqueoAvisaDeReservasAfectadas() {
        UUID r = recurso(negocioA, "104");
        reservas.conReservaConfirmada(r, new ReservaAfectada(UUID.randomUUID(), "RES-0007",
                OffsetDateTime.parse("2026-03-02T14:00:00Z"),
                OffsetDateTime.parse("2026-03-04T11:00:00Z"), "Ada Lovelace"));

        BloqueoCreado creado = enContexto(negocioA, admin, ADMIN,
                () -> bloqueos.crear(r, entre(MAR_1, MAR_5)));

        assertThat(creado.hayReservasAfectadas()).isTrue();
        assertThat(creado.reservasAfectadas()).extracting(ReservaAfectada::codigo)
                .containsExactly("RES-0007");
        // El bloqueo quedó creado a pesar del conflicto.
        assertThat(enContexto(negocioA, admin, ADMIN, () -> bloqueos.listar(r))).hasSize(1);
    }

    @Test
    @DisplayName("Criterio 3: sin reservas en el periodo, no hay nada que advertir")
    void bloqueoSinConflictoNoAdvierteNada() {
        UUID r = recurso(negocioA, "105");
        reservas.conReservaConfirmada(r, new ReservaAfectada(UUID.randomUUID(), "RES-0009",
                MAR_10, MAR_12, "Grace Hopper"));

        BloqueoCreado creado = enContexto(negocioA, admin, ADMIN,
                () -> bloqueos.crear(r, entre(MAR_1, MAR_5)));

        assertThat(creado.hayReservasAfectadas()).isFalse();
    }

    @Test
    @DisplayName("Levantar el bloqueo devuelve el recurso a disponible")
    void eliminarLiberaElPeriodo() {
        UUID r = recurso(negocioA, "106");
        BloqueoCreado creado = enContexto(negocioA, admin, ADMIN,
                () -> bloqueos.crear(r, entre(MAR_1, MAR_5)));

        enContexto(negocioA, admin, ADMIN, () -> bloqueos.eliminar(creado.bloqueo().id()));

        DisponibilidadDeRecurso libre = enContexto(negocioA, admin, ADMIN,
                () -> bloqueos.disponibilidad(r, MAR_1, MAR_5));
        assertThat(libre.disponible()).isTrue();
        assertThat(enContexto(negocioA, admin, ADMIN, () -> bloqueos.listar(r))).isEmpty();
    }

    @Test
    @DisplayName("El segundo negocio no ve ni toca los bloqueos ni los recursos del primero")
    void aislamientoEntreNegocios() {
        UUID recursoA = recurso(negocioA, "201");
        BloqueoCreado enA = enContexto(negocioA, admin, ADMIN,
                () -> bloqueos.crear(recursoA, entre(MAR_1, MAR_5)));

        UUID recursoB = recurso(negocioB, "201");
        enContexto(negocioB, admin, ADMIN, () -> bloqueos.crear(recursoB, entre(MAR_1, MAR_5)));

        // B pide los bloqueos de un recurso que para él no existe.
        assertThatThrownBy(() -> enContexto(negocioB, admin, ADMIN, () -> bloqueos.listar(recursoA)))
                .isInstanceOf(NoEncontradoException.class);
        // B no puede levantar el bloqueo de A.
        assertThatThrownBy(() -> enContexto(negocioB, admin, ADMIN,
                () -> bloqueos.eliminar(enA.bloqueo().id())))
                .isInstanceOf(NoEncontradoException.class);
        // El bloqueo de A sigue en pie.
        assertThat(enContexto(negocioA, admin, ADMIN, () -> bloqueos.listar(recursoA))).hasSize(1);
        assertThat(enContexto(negocioB, admin, ADMIN, () -> bloqueos.listar(recursoB))).hasSize(1);
    }

    @Test
    @DisplayName("Un bloqueo sobre un recurso inexistente responde 404")
    void recursoInexistente404() {
        assertThatThrownBy(() -> enContexto(negocioA, admin, ADMIN,
                () -> bloqueos.crear(UUID.randomUUID(), entre(MAR_1, MAR_5))))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    @DisplayName("Un periodo con el fin antes del inicio responde 422")
    void periodoInvalido422() {
        UUID r = recurso(negocioA, "107");
        assertThatThrownBy(() -> enContexto(negocioA, admin, ADMIN,
                () -> bloqueos.crear(r, entre(MAR_5, MAR_1))))
                .isInstanceOf(ReglaDeNegocioException.class);
    }
}
