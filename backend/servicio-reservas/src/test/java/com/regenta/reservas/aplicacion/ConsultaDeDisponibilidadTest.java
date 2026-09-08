package com.regenta.reservas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.reservas.BaseDeReservas;
import com.regenta.reservas.aplicacion.DisponibilidadEnPeriodo.RecursoLibre;
import com.regenta.reservas.aplicacion.DisponibilidadEnPeriodo.RecursoOcupado;
import com.regenta.reservas.infra.CatalogoDeRecursosStub;

/**
 * HU-069. Disponibilidad en un periodo, con dos negocios cargados: lo que ocupa
 * a un negocio no ocupa al otro (la consulta a {@code reservas} va con RLS).
 */
class ConsultaDeDisponibilidadTest extends BaseDeReservas {

    private static final Set<String> RECEPCION = Set.of("RESERVAS_RESERVA_VER");

    private static final OffsetDateTime D1_15 = OffsetDateTime.parse("2026-07-01T15:00:00Z");
    private static final OffsetDateTime D2_11 = OffsetDateTime.parse("2026-07-02T11:00:00Z");
    private static final OffsetDateTime D2_20 = OffsetDateTime.parse("2026-07-02T20:00:00Z");
    private static final OffsetDateTime D3_11 = OffsetDateTime.parse("2026-07-03T11:00:00Z");

    @Autowired
    private ConsultaDeDisponibilidad disponibilidad;
    @Autowired
    private CatalogoDeRecursosStub catalogo;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID recepcion = UUID.randomUUID();
    private final AtomicInteger numero = new AtomicInteger();

    @BeforeEach
    void limpiar() {
        catalogo.reiniciar();
    }

    private void reserva(UUID negocio, UUID recursoId, UUID tipoRecursoId, OffsetDateTime desde,
            OffsetDateTime hasta, String estado) {
        ejecutarComoElServicio(negocio, "INSERT INTO reservas "
                + "(id, negocio_id, numero, tipo_recurso_id, recurso_id, periodo, estado) VALUES ('"
                + UUID.randomUUID() + "', '" + negocio + "', 'R-" + numero.incrementAndGet() + "', '"
                + tipoRecursoId + "', '" + recursoId + "', "
                + "tstzrange('" + desde + "', '" + hasta + "', '[)'), '" + estado + "')");
    }

    private DisponibilidadEnPeriodo consultar(UUID negocio, OffsetDateTime desde,
            OffsetDateTime hasta, UUID tipoRecursoId, Integer personas) {
        return enContexto(negocio, recepcion, RECEPCION, () -> disponibilidad.consultar(
                new SolicitudDeDisponibilidad(desde, hasta, tipoRecursoId, personas)));
    }

    @Test
    @DisplayName("Criterio 1: se excluyen los recursos con reserva solapada y con bloqueo")
    void excluyeReservadosYBloqueados() {
        UUID tipo = UUID.randomUUID();
        UUID conReserva = catalogo.agregar(negocioA, tipo, "101", 2, 0, 0).id();
        UUID conBloqueo = catalogo.agregar(negocioA, tipo, "102", 2, 0, 0).id();
        UUID libre = catalogo.agregar(negocioA, tipo, "103", 2, 0, 0).id();

        reserva(negocioA, conReserva, tipo, D1_15, D2_11, "CONFIRMADA");
        catalogo.bloquear(conBloqueo, D1_15, D3_11);

        DisponibilidadEnPeriodo res = consultar(negocioA,
                OffsetDateTime.parse("2026-07-01T18:00:00Z"),
                OffsetDateTime.parse("2026-07-02T10:00:00Z"), tipo, null);

        assertThat(res.libres()).extracting(RecursoLibre::recursoId).containsExactly(libre);
        assertThat(res.ocupados()).extracting(RecursoOcupado::recursoId, RecursoOcupado::motivo)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(conReserva, "RESERVA"),
                        org.assertj.core.groups.Tuple.tuple(conBloqueo, "BLOQUEO"));
        assertThat(res.totalRecursos()).isEqualTo(3);
    }

    @Test
    @DisplayName("Criterio 2: una reserva CANCELADA o NO_SHOW deja el recurso libre")
    void canceladasYNoShowNoOcupan() {
        UUID tipo = UUID.randomUUID();
        UUID r = catalogo.agregar(negocioA, tipo, "201", 2, 0, 0).id();
        reserva(negocioA, r, tipo, D1_15, D2_11, "CANCELADA");
        reserva(negocioA, r, tipo, D1_15, D2_11, "NO_SHOW");

        DisponibilidadEnPeriodo res = consultar(negocioA, D1_15, D2_11, tipo, null);

        assertThat(res.libres()).extracting(RecursoLibre::recursoId).containsExactly(r);
        assertThat(res.ocupados()).isEmpty();
    }

    @Test
    @DisplayName("Criterio 3: un check-out a las 11:00 deja el recurso libre desde las 11:00 (sin buffer)")
    void checkoutLiberaSinBuffer() {
        UUID tipo = UUID.randomUUID();
        UUID r = catalogo.agregar(negocioA, tipo, "301", 2, 0, 0).id();
        reserva(negocioA, r, tipo, D1_15, D2_11, "CONFIRMADA");

        assertThat(consultar(negocioA, D2_11, D2_20, tipo, null).libres())
                .extracting(RecursoLibre::recursoId).containsExactly(r);
    }

    @Test
    @DisplayName("Criterio 4: con buffer de limpieza, el recurso sigue ocupado hasta que pasa el buffer")
    void bufferDeLimpiezaSeRespeta() {
        UUID tipo = UUID.randomUUID();
        UUID r = catalogo.agregar(negocioA, tipo, "401", 2, 0, 120).id();
        reserva(negocioA, r, tipo, D1_15, D2_11, "CONFIRMADA");

        // Consulta desde las 11:00: ocupado hasta las 13:00 por la limpieza.
        assertThat(consultar(negocioA, D2_11, D2_20, tipo, null).ocupados())
                .extracting(RecursoOcupado::recursoId).containsExactly(r);
        // Consulta desde las 13:00: libre.
        assertThat(consultar(negocioA, OffsetDateTime.parse("2026-07-02T13:00:00Z"), D2_20, tipo,
                null).libres()).extracting(RecursoLibre::recursoId).containsExactly(r);
    }

    @Test
    @DisplayName("Una reserva que empieza justo cuando termina otra no cuenta como solape")
    void adyacenteSinSolape() {
        UUID tipo = UUID.randomUUID();
        UUID r = catalogo.agregar(negocioA, tipo, "501", 2, 0, 0).id();
        reserva(negocioA, r, tipo, D1_15, D2_11, "CONFIRMADA");

        assertThat(consultar(negocioA, D2_11, D3_11, tipo, null).libres())
                .extracting(RecursoLibre::recursoId).containsExactly(r);
    }

    @Test
    @DisplayName("El filtro por personas descarta los recursos que no dan la capacidad")
    void filtraPorCapacidad() {
        UUID tipo = UUID.randomUUID();
        UUID chico = catalogo.agregar(negocioA, tipo, "601", 2, 0, 0).id();
        UUID grande = catalogo.agregar(negocioA, tipo, "602", 4, 0, 0).id();

        DisponibilidadEnPeriodo res = consultar(negocioA, D1_15, D2_11, tipo, 3);

        assertThat(res.libres()).extracting(RecursoLibre::recursoId).containsExactly(grande);
        assertThat(res.ocupados()).isEmpty();
        assertThat(chico).isNotNull();
    }

    @Test
    @DisplayName("Un periodo con el fin antes del inicio responde 422")
    void periodoInvalido() {
        assertThatThrownBy(() -> consultar(negocioA, D2_11, D1_15, null, null))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("La ocupación de un negocio no afecta al otro: la consulta a reservas va con RLS")
    void aislamientoEntreNegocios() {
        UUID tipo = UUID.randomUUID();
        UUID r = catalogo.agregar(negocioA, tipo, "701", 2, 0, 0).id();
        reserva(negocioA, r, tipo, D1_15, D2_11, "CONFIRMADA");

        assertThat(consultar(negocioA, D1_15, D2_11, tipo, null).ocupados())
                .extracting(RecursoOcupado::recursoId).containsExactly(r);
        // El negocio B ve el mismo recurso (stub) pero no tiene la reserva.
        assertThat(consultar(negocioB, D1_15, D2_11, tipo, null).libres())
                .extracting(RecursoLibre::recursoId).containsExactly(r);
    }

    @Test
    @DisplayName("Criterio 5: 200 recursos y 6 meses de reservas, consultar un mes responde en < 500 ms")
    void respondeRapidoConVolumen() {
        UUID tipo = UUID.randomUUID();
        StringBuilder insert = new StringBuilder("INSERT INTO reservas "
                + "(id, negocio_id, numero, tipo_recurso_id, recurso_id, periodo, estado) VALUES ");
        int fila = 0;
        for (int i = 0; i < 200; i++) {
            UUID r = catalogo.agregar(negocioA, tipo, "H" + i, 2, 0, 0).id();
            // Una reserva cada dos semanas durante ~6 meses, sin solaparse entre sí.
            for (int quincena = 0; quincena < 13; quincena++) {
                OffsetDateTime entrada = OffsetDateTime.parse("2026-01-05T15:00:00Z")
                        .plusWeeks(quincena * 2L);
                OffsetDateTime salida = entrada.plusDays(3);
                if (fila++ > 0) {
                    insert.append(", ");
                }
                insert.append("('").append(UUID.randomUUID()).append("', '").append(negocioA)
                        .append("', 'V-").append(fila).append("', '").append(tipo).append("', '")
                        .append(r).append("', tstzrange('").append(entrada).append("', '")
                        .append(salida).append("', '[)'), 'CONFIRMADA')");
            }
        }
        ejecutarComoElServicio(negocioA, insert.toString());

        OffsetDateTime desde = OffsetDateTime.parse("2026-03-01T00:00:00Z");
        OffsetDateTime hasta = OffsetDateTime.parse("2026-04-01T00:00:00Z");

        long inicio = System.nanoTime();
        DisponibilidadEnPeriodo res = consultar(negocioA, desde, hasta, tipo, null);
        long ms = (System.nanoTime() - inicio) / 1_000_000;

        assertThat(res.totalRecursos()).isEqualTo(200);
        assertThat(res.libres().size() + res.ocupados().size()).isEqualTo(200);
        assertThat(ms).isLessThan(500);
    }
}
