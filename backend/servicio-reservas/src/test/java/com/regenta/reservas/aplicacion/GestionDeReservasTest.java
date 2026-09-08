package com.regenta.reservas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.reservas.BaseDeReservas;
import com.regenta.reservas.aplicacion.CotizacionDeEstadia.NocheCotizada;
import com.regenta.reservas.infra.CatalogoDeRecursosStub;

/**
 * HU-070. Crear reservas sin overbooking, con dos negocios cargados. El rechazo
 * del solape lo hace PostgreSQL con el {@code EXCLUDE USING gist}: hay un test
 * directo contra la base, uno del 409 del servicio y uno de concurrencia.
 */
class GestionDeReservasTest extends BaseDeReservas {

    private static final java.util.Set<String> RECEPCION = java.util.Set.of(
            "RESERVAS_RESERVA_VER", "RESERVAS_RESERVA_CREAR");

    private static final OffsetDateTime D1 = OffsetDateTime.parse("2026-08-01T15:00:00Z");
    private static final OffsetDateTime D2 = OffsetDateTime.parse("2026-08-02T11:00:00Z");
    private static final OffsetDateTime D3 = OffsetDateTime.parse("2026-08-03T11:00:00Z");
    private static final OffsetDateTime D4 = OffsetDateTime.parse("2026-08-04T11:00:00Z");

    @Autowired
    private GestionDeReservas reservas;
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

    private SolicitudDeReserva solicitud(UUID recursoId, OffsetDateTime desde,
            OffsetDateTime hasta) {
        return new SolicitudDeReserva(desde, hasta, tipo, recursoId, null, null, 2, 0, null,
                "MOSTRADOR", null);
    }

    private ReservaDelNegocio crear(UUID negocio, SolicitudDeReserva solicitud) {
        return enContexto(negocio, recepcion, RECEPCION, () -> reservas.crear(solicitud));
    }

    @Test
    @DisplayName("Criterio 2: el servicio traduce el solape con una reserva a un 409")
    void solapeConReservaDaConflicto() {
        UUID recurso = UUID.randomUUID();
        crear(negocioA, solicitud(recurso, D1, D3));

        assertThatThrownBy(() -> crear(negocioA, solicitud(recurso, D2, D4)))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("Criterio 2: PostgreSQL rechaza por el EXCLUDE un INSERT directo que se solapa")
    void postgresRechazaElSolape() {
        UUID recurso = UUID.randomUUID();
        UUID reservaId = crear(negocioA, solicitud(recurso, D1, D3)).id();
        assertThat(reservaId).isNotNull();

        assertThatThrownBy(() -> ejecutarComoElServicio(negocioA, "INSERT INTO reservas "
                + "(id, negocio_id, numero, tipo_recurso_id, recurso_id, periodo) VALUES ('"
                + UUID.randomUUID() + "', '" + negocioA + "', 'X-1', '" + tipo + "', '" + recurso
                + "', tstzrange('2026-08-02 11:00+00', '2026-08-05 11:00+00', '[)'))"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Criterio 3: una reserva que empieza cuando termina otra se permite")
    void adyacentesSePermiten() {
        UUID recurso = UUID.randomUUID();
        crear(negocioA, solicitud(recurso, D1, D2));

        assertThatCode(() -> crear(negocioA, solicitud(recurso, D2, D3)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Criterio 1: dos reservas concurrentes del mismo recurso y periodo, solo una gana")
    void concurrenciaSoloUnaGana() throws Exception {
        UUID recurso = UUID.randomUUID();
        SolicitudDeReserva misma = solicitud(recurso, D1, D3);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch listos = new CountDownLatch(2);
        CountDownLatch ya = new CountDownLatch(1);
        Callable<Object> intento = () -> {
            listos.countDown();
            ya.await();
            try {
                return crear(negocioA, misma);
            } catch (RuntimeException e) {
                return e;
            }
        };
        Future<Object> f1 = pool.submit(intento);
        Future<Object> f2 = pool.submit(intento);
        listos.await();
        ya.countDown();
        Object r1 = f1.get();
        Object r2 = f2.get();
        pool.shutdown();

        long exitos = List.of(r1, r2).stream().filter(o -> o instanceof ReservaDelNegocio).count();
        long conflictos = List.of(r1, r2).stream()
                .filter(o -> o instanceof ConflictoDeEstadoException).count();
        assertThat(exitos).isEqualTo(1);
        assertThat(conflictos).isEqualTo(1);
    }

    @Test
    @DisplayName("Criterio 4: el subtotal sale de la cotización noche por noche de servicio-recursos")
    void cotizacionPorNoche() {
        UUID recurso = UUID.randomUUID();
        UUID t1 = UUID.randomUUID();
        UUID t2 = UUID.randomUUID();
        catalogo.cotizacion(recurso, new CotizacionDeEstadia(new BigDecimal("450000"), "COP", true,
                List.of(
                        new NocheCotizada(LocalDate.of(2026, 8, 1), t1, new BigDecimal("150000")),
                        new NocheCotizada(LocalDate.of(2026, 8, 2), t2, new BigDecimal("150000")),
                        new NocheCotizada(LocalDate.of(2026, 8, 3), t2, new BigDecimal("150000")))));

        ReservaDelNegocio r = crear(negocioA, solicitud(recurso, D1, D4));

        assertThat(r.subtotal()).isEqualByComparingTo("450000");
        assertThat(r.total()).isEqualByComparingTo("450000");
        assertThat(r.tarifaId()).isEqualTo(t1);
        assertThat(r.nochesCotizadas()).extracting(ReservaDelNegocio.NocheDeReserva::tarifaId)
                .containsExactly(t1, t2, t2);
    }

    @Test
    @DisplayName("Criterio 5: al crear la reserva se calcula el anticipo requerido de la política")
    void anticipoRequerido() {
        UUID recurso = UUID.randomUUID();
        UUID politica = UUID.randomUUID();
        catalogo.cotizacion(recurso, new CotizacionDeEstadia(new BigDecimal("500000"), "COP", true,
                List.of(new NocheCotizada(LocalDate.of(2026, 8, 1), UUID.randomUUID(),
                        new BigDecimal("500000")))));
        catalogo.anticipoPct(new BigDecimal("0.30"), politica);

        ReservaDelNegocio r = crear(negocioA, solicitud(recurso, D1, D2));

        assertThat(r.anticipoRequerido()).isEqualByComparingTo("150000");
        assertThat(r.saldo()).isEqualByComparingTo("350000");
        assertThat(r.politicaCancelacionId()).isEqualTo(politica);
    }

    @Test
    @DisplayName("Criterio 6: un periodo con fin anterior o igual al inicio responde 422")
    void periodoInvalido() {
        assertThatThrownBy(() -> crear(negocioA, solicitud(UUID.randomUUID(), D3, D1)))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("El número de reserva corre por negocio")
    void numeroCorrido() {
        String uno = crear(negocioA, solicitud(UUID.randomUUID(), D1, D2)).numero();
        String dos = crear(negocioA, solicitud(UUID.randomUUID(), D1, D2)).numero();

        assertThat(uno).isEqualTo("RES-1");
        assertThat(dos).isEqualTo("RES-2");
        assertThat(crear(negocioB, solicitud(UUID.randomUUID(), D1, D2)).numero()).isEqualTo("RES-1");
    }

    @Test
    @DisplayName("El segundo negocio no ve la reserva del primero, y puede reservar el mismo recurso")
    void aislamientoEntreNegocios() {
        UUID recurso = UUID.randomUUID();
        UUID enA = crear(negocioA, solicitud(recurso, D1, D3)).id();

        assertThatThrownBy(() -> enContexto(negocioB, recepcion, RECEPCION,
                () -> reservas.ver(enA))).isInstanceOf(NoEncontradoException.class);
        // El EXCLUDE lleva negocio_id: B puede reservar el mismo recurso y periodo.
        assertThatCode(() -> crear(negocioB, solicitud(recurso, D1, D3)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Crear una reserva publica reserva_creada al outbox")
    void publicaReservaCreada() {
        UUID enA = crear(negocioA, solicitud(UUID.randomUUID(), D1, D3)).id();

        assertThat(contar("SELECT count(*) FROM outbox_eventos WHERE agregado_id = '" + enA
                + "' AND tipo_evento = 'reserva_creada'")).isEqualTo(1);
    }
}
