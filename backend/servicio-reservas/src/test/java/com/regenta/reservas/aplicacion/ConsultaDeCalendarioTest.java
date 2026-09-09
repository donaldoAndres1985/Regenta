package com.regenta.reservas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.reservas.BaseDeReservas;
import com.regenta.reservas.aplicacion.CalendarioDeOcupacion.BarraDeBloqueo;
import com.regenta.reservas.aplicacion.CalendarioDeOcupacion.BarraDeReserva;
import com.regenta.reservas.aplicacion.CalendarioDeOcupacion.RecursoDeCalendario;
import com.regenta.reservas.infra.CatalogoDeRecursosStub;

/** HU-075. El calendario junta recursos, reservas y bloqueos de una franja de días. */
class ConsultaDeCalendarioTest extends BaseDeReservas {

    private static final java.util.Set<String> RECEPCION = java.util.Set.of("RESERVAS_RESERVA_VER");

    @Autowired
    private ConsultaDeCalendario calendario;
    @Autowired
    private CatalogoDeRecursosStub catalogo;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID recepcion = UUID.randomUUID();
    private final UUID tipo = UUID.randomUUID();
    private final AtomicInteger numero = new AtomicInteger();

    @BeforeEach
    void limpiar() {
        catalogo.reiniciar();
    }

    private void reserva(UUID negocio, UUID recursoId, LocalDate entrada, int noches,
            String estado) {
        OffsetDateTime desde = entrada.atTime(15, 0).atOffset(java.time.ZoneOffset.UTC);
        OffsetDateTime hasta = entrada.plusDays(noches).atTime(11, 0)
                .atOffset(java.time.ZoneOffset.UTC);
        ejecutarComoElServicio(negocio, "INSERT INTO reservas "
                + "(id, negocio_id, numero, tipo_recurso_id, recurso_id, periodo, estado) VALUES ('"
                + UUID.randomUUID() + "', '" + negocio + "', 'R-" + numero.incrementAndGet() + "', '"
                + tipo + "', '" + recursoId + "', tstzrange('" + desde + "', '" + hasta + "', '[)'), '"
                + estado + "')");
    }

    private CalendarioDeOcupacion ver(UUID negocio, LocalDate desde, LocalDate hasta) {
        return enContexto(negocio, recepcion, RECEPCION,
                () -> calendario.calendario(desde, hasta, tipo));
    }

    @Test
    @DisplayName("Criterio 1: una fila por recurso, una columna por día, las reservas como barras")
    void filasColumnasYBarras() {
        UUID r1 = catalogo.agregar(negocioA, tipo, "101", 2, 0, 0).id();
        UUID r2 = catalogo.agregar(negocioA, tipo, "102", 2, 0, 0).id();
        reserva(negocioA, r1, LocalDate.of(2026, 11, 3), 2, "CONFIRMADA");

        CalendarioDeOcupacion cal = ver(negocioA, LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 7));

        assertThat(cal.recursos()).extracting(RecursoDeCalendario::id)
                .containsExactlyInAnyOrder(r1, r2);
        assertThat(cal.dias()).hasSize(7).startsWith(LocalDate.of(2026, 11, 1))
                .endsWith(LocalDate.of(2026, 11, 7));
        assertThat(cal.reservas()).singleElement().satisfies(b -> {
            assertThat(b.recursoId()).isEqualTo(r1);
            assertThat(b.estado()).isEqualTo("CONFIRMADA");
        });
    }

    @Test
    @DisplayName("Criterio 2: cada barra trae el estado de su reserva")
    void barrasConEstado() {
        UUID r = catalogo.agregar(negocioA, tipo, "201", 2, 0, 0).id();
        reserva(negocioA, r, LocalDate.of(2026, 11, 2), 1, "CONFIRMADA");
        reserva(negocioA, r, LocalDate.of(2026, 11, 5), 1, "PENDIENTE");

        CalendarioDeOcupacion cal = ver(negocioA, LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 8));

        assertThat(cal.reservas()).extracting(BarraDeReserva::estado)
                .containsExactlyInAnyOrder("CONFIRMADA", "PENDIENTE");
    }

    @Test
    @DisplayName("Criterio 4: los bloqueos vienen aparte de las reservas, con su motivo")
    void bloqueosAparte() {
        UUID r = catalogo.agregar(negocioA, tipo, "301", 2, 0, 0).id();
        catalogo.bloquear(r, LocalDate.of(2026, 11, 4).atStartOfDay().atOffset(java.time.ZoneOffset.UTC),
                LocalDate.of(2026, 11, 6).atStartOfDay().atOffset(java.time.ZoneOffset.UTC),
                "MANTENIMIENTO");

        CalendarioDeOcupacion cal = ver(negocioA, LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 7));

        assertThat(cal.reservas()).isEmpty();
        assertThat(cal.bloqueos()).singleElement().satisfies(b -> {
            assertThat(b.recursoId()).isEqualTo(r);
            assertThat(b.motivo()).isEqualTo("MANTENIMIENTO");
        });
    }

    @Test
    @DisplayName("Las canceladas y no-show no aparecen en el calendario")
    void canceladasFuera() {
        UUID r = catalogo.agregar(negocioA, tipo, "401", 2, 0, 0).id();
        reserva(negocioA, r, LocalDate.of(2026, 11, 3), 2, "CANCELADA");
        reserva(negocioA, r, LocalDate.of(2026, 11, 3), 2, "NO_SHOW");

        assertThat(ver(negocioA, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 7)).reservas())
                .isEmpty();
    }

    @Test
    @DisplayName("El calendario de un negocio no muestra las reservas del otro")
    void aislamientoEntreNegocios() {
        UUID r = catalogo.agregar(negocioA, tipo, "501", 2, 0, 0).id();
        reserva(negocioA, r, LocalDate.of(2026, 11, 3), 2, "CONFIRMADA");

        assertThat(ver(negocioA, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 7)).reservas())
                .hasSize(1);
        assertThat(ver(negocioB, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 7)).reservas())
                .isEmpty();
    }

    @Test
    @DisplayName("Un rango invertido o demasiado ancho responde 422")
    void rangoInvalido() {
        assertThatThrownBy(() -> ver(negocioA, LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 1)))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> ver(negocioA, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 1)))
                .isInstanceOf(ReglaDeNegocioException.class);
    }
}
