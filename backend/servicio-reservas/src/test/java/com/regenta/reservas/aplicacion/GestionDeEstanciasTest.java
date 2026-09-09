package com.regenta.reservas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.reservas.BaseDeReservas;
import com.regenta.reservas.infra.CatalogoDeRecursosStub;

/** HU-072. Check-in con asignación de recurso y apertura de estancia, con dos negocios. */
class GestionDeEstanciasTest extends BaseDeReservas {

    private static final java.util.Set<String> RECEPCION = java.util.Set.of(
            "RESERVAS_RESERVA_VER", "RESERVAS_RESERVA_CREAR", "RESERVAS_RESERVA_EDITAR");

    @Autowired
    private GestionDeReservas reservas;
    @Autowired
    private GestionDeEstancias estancias;
    @Autowired
    private CatalogoDeRecursosStub catalogo;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID recepcion = UUID.randomUUID();
    private final UUID tipo = UUID.randomUUID();

    @BeforeEach
    void limpiar() {
        catalogo.reiniciar();
    }

    private OffsetDateTime enDias(int dias) {
        return OffsetDateTime.now(ZoneOffset.UTC).plusDays(dias).withNano(0);
    }

    private ReservaDelNegocio reservaConfirmada(UUID negocio, UUID recursoId, OffsetDateTime desde,
            OffsetDateTime hasta) {
        ReservaDelNegocio r = enContexto(negocio, recepcion, RECEPCION, () -> reservas.crear(
                new SolicitudDeReserva(desde, hasta, tipo, recursoId, null, null, 2, 0, null,
                        "MOSTRADOR", null)));
        enContexto(negocio, recepcion, RECEPCION, () -> reservas.confirmar(r.id()));
        return r;
    }

    private SolicitudDeCheckIn checkIn(UUID recursoId, SolicitudDeOcupante... ocupantes) {
        return new SolicitudDeCheckIn(recursoId, new BigDecimal("50000"), "sin novedad",
                List.of(ocupantes));
    }

    private SolicitudDeOcupante titular() {
        return new SolicitudDeOcupante(true, "Ada", "Lovelace", "CC", "51234567", "CO", null,
                null, null);
    }

    @Test
    @DisplayName("Criterio 1: una reserva vendida por tipo asigna un recurso libre de ese tipo en el check-in")
    void asignaRecursoLibreDelTipo() {
        UUID libre = catalogo.agregar(negocioA, tipo, "101", 2, 0, 0).id();
        ReservaDelNegocio r = reservaConfirmada(negocioA, null, enDias(10), enDias(12));
        assertThat(r.recursoId()).isNull();

        EstanciaDelNegocio e = enContexto(negocioA, recepcion, RECEPCION,
                () -> estancias.checkIn(r.id(), checkIn(null, titular())));

        assertThat(e.recursoAsignadoId()).isEqualTo(libre);
        assertThat(enContexto(negocioA, recepcion, RECEPCION, () -> reservas.ver(r.id())).recursoId())
                .isEqualTo(libre);
    }

    @Test
    @DisplayName("Criterio 2: asignar un recurso ya ocupado en ese periodo lo rechaza el constraint")
    void recursoOcupadoLoRechazaElConstraint() {
        UUID recurso = UUID.randomUUID();
        reservaConfirmada(negocioA, recurso, enDias(20), enDias(23)); // ocupa el recurso
        ReservaDelNegocio porTipo = reservaConfirmada(negocioA, null, enDias(21), enDias(24));

        assertThatThrownBy(() -> enContexto(negocioA, recepcion, RECEPCION,
                () -> estancias.checkIn(porTipo.id(), checkIn(recurso, titular()))))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("Criterio 3: el check-in abre la estancia y pasa la reserva a CHECK_IN")
    void abreEstanciaYMueveEstado() {
        UUID recurso = UUID.randomUUID();
        ReservaDelNegocio r = reservaConfirmada(negocioA, recurso, enDias(5), enDias(8));

        EstanciaDelNegocio e = enContexto(negocioA, recepcion, RECEPCION,
                () -> estancias.checkIn(r.id(), checkIn(null, titular())));

        assertThat(e.estado()).isEqualTo("EN_CURSO");
        assertThat(e.checkOutPrevisto()).isEqualTo(r.hasta());
        assertThat(e.deposito()).isEqualByComparingTo("50000");
        assertThat(enContexto(negocioA, recepcion, RECEPCION, () -> reservas.ver(r.id())).estado())
                .isEqualTo("CHECK_IN");
        assertThat(contar("SELECT count(*) FROM reserva_eventos WHERE reserva_id = '" + r.id()
                + "' AND estado_nuevo = 'CHECK_IN'")).isEqualTo(1);
    }

    @Test
    @DisplayName("Criterio 4: el titular queda registrado con su documento")
    void titularConDocumento() {
        UUID recurso = UUID.randomUUID();
        ReservaDelNegocio r = reservaConfirmada(negocioA, recurso, enDias(9), enDias(11));

        EstanciaDelNegocio e = enContexto(negocioA, recepcion, RECEPCION,
                () -> estancias.checkIn(r.id(), checkIn(null, titular(),
                        new SolicitudDeOcupante(false, "Grace", "Hopper", null, null, null, null,
                                null, null))));

        assertThat(e.ocupantes()).hasSize(2);
        OcupanteDelNegocio titular = e.ocupantes().stream().filter(OcupanteDelNegocio::esTitular)
                .findFirst().orElseThrow();
        assertThat(titular.numeroDocumento()).isEqualTo("51234567");
        assertThat(titular.tipoDocumento()).isEqualTo("CC");
    }

    @Test
    @DisplayName("Criterio 4: un titular sin documento en el check-in responde 422")
    void titularSinDocumento422() {
        UUID recurso = UUID.randomUUID();
        ReservaDelNegocio r = reservaConfirmada(negocioA, recurso, enDias(13), enDias(15));

        assertThatThrownBy(() -> enContexto(negocioA, recepcion, RECEPCION,
                () -> estancias.checkIn(r.id(), checkIn(null,
                        new SolicitudDeOcupante(true, "Ada", "Lovelace", null, null, null, null,
                                null, null)))))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Criterio 5: el check-in publica check_in_registrado con el recurso")
    void publicaCheckIn() {
        UUID recurso = UUID.randomUUID();
        ReservaDelNegocio r = reservaConfirmada(negocioA, recurso, enDias(16), enDias(18));

        enContexto(negocioA, recepcion, RECEPCION,
                () -> estancias.checkIn(r.id(), checkIn(null, titular())));

        assertThat(consultar("SELECT payload FROM outbox_eventos WHERE agregado_id = '" + r.id()
                + "' AND tipo_evento = 'check_in_registrado'").get(0))
                .contains("\"recurso_id\": \"" + recurso + "\"");
    }

    @Test
    @DisplayName("No se hace check-in de una reserva no confirmada, ni dos veces")
    void checkInInvalido() {
        UUID recurso = UUID.randomUUID();
        ReservaDelNegocio pendiente = enContexto(negocioA, recepcion, RECEPCION,
                () -> reservas.crear(new SolicitudDeReserva(enDias(25), enDias(27), tipo, recurso,
                        null, null, 2, 0, null, "MOSTRADOR", null)));
        assertThatThrownBy(() -> enContexto(negocioA, recepcion, RECEPCION,
                () -> estancias.checkIn(pendiente.id(), checkIn(null, titular()))))
                .isInstanceOf(ConflictoDeEstadoException.class);

        ReservaDelNegocio r = reservaConfirmada(negocioA, UUID.randomUUID(), enDias(28), enDias(30));
        enContexto(negocioA, recepcion, RECEPCION,
                () -> estancias.checkIn(r.id(), checkIn(null, titular())));
        assertThatThrownBy(() -> enContexto(negocioA, recepcion, RECEPCION,
                () -> estancias.checkIn(r.id(), checkIn(null, titular()))))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("Sin recursos libres de ese tipo, el check-in responde 409")
    void sinRecursosLibres() {
        ReservaDelNegocio r = reservaConfirmada(negocioA, null, enDias(31), enDias(33));

        assertThatThrownBy(() -> enContexto(negocioA, recepcion, RECEPCION,
                () -> estancias.checkIn(r.id(), checkIn(null, titular()))))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("El segundo negocio no puede hacer check-in de la reserva del primero")
    void aislamientoEntreNegocios() {
        UUID enA = reservaConfirmada(negocioA, UUID.randomUUID(), enDias(4), enDias(6)).id();

        assertThatThrownBy(() -> enContexto(negocioB, recepcion, RECEPCION,
                () -> estancias.checkIn(enA, checkIn(null, titular()))))
                .isInstanceOf(NoEncontradoException.class);
    }
}
