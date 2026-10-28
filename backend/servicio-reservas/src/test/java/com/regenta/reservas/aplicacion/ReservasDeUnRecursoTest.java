package com.regenta.reservas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.errores.SinPermisoException;
import com.regenta.reservas.BaseDeReservas;

/**
 * HU-138. Lo que servicio-recursos le pregunta a servicio-reservas: si un recurso
 * tiene reservas por delante (criterio 4, para no borrarlo) y qué reservas
 * confirmadas pisa un bloqueo (criterio 5). Con dos negocios cargados.
 */
class ReservasDeUnRecursoTest extends BaseDeReservas {

    private static final Set<String> VER = Set.of("RESERVAS_RESERVA_VER");

    @Autowired
    private ReservasDeUnRecurso reservas;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();
    private final UUID tipo = UUID.randomUUID();
    private final UUID recurso = UUID.randomUUID();
    private final AtomicInteger numero = new AtomicInteger();

    // Al segundo: PostgreSQL guarda microsegundos y la comparación sería frágil.
    private static final OffsetDateTime AHORA = OffsetDateTime.now(ZoneOffset.UTC)
            .truncatedTo(java.time.temporal.ChronoUnit.SECONDS);

    private String reserva(UUID negocio, UUID recursoId, OffsetDateTime desde,
            OffsetDateTime hasta, String estado) {
        String codigo = "R-" + negocio.toString().substring(0, 4) + "-" + numero.incrementAndGet();
        ejecutarComoElServicio(negocio, "INSERT INTO reservas "
                + "(id, negocio_id, numero, tipo_recurso_id, recurso_id, periodo, estado) VALUES ('"
                + UUID.randomUUID() + "', '" + negocio + "', '" + codigo + "', '" + tipo + "', '"
                + recursoId + "', tstzrange('" + desde + "', '" + hasta + "', '[)'), '" + estado
                + "')");
        return codigo;
    }

    @Test
    @DisplayName("Criterio 4: un recurso con una reserva por delante que lo ocupa tiene reservas futuras")
    void reservaFutura() {
        String futura = reserva(negocioA, recurso, AHORA.plusDays(3), AHORA.plusDays(5),
                "CONFIRMADA");
        reserva(negocioA, recurso, AHORA.minusDays(10), AHORA.minusDays(8), "CHECK_OUT");

        var porDelante = enContexto(negocioA, admin, VER,
                () -> reservas.delRecurso(recurso, AHORA, null, false));

        assertThat(porDelante).extracting(ReservaDeUnRecurso::numero).containsExactly(futura);
    }

    @Test
    @DisplayName("Criterio 4: las canceladas, no-show, expiradas y ya cerradas no cuentan como reservas futuras")
    void lasQueNoOcupanNoCuentan() {
        reserva(negocioA, recurso, AHORA.plusDays(3), AHORA.plusDays(5), "CANCELADA");
        reserva(negocioA, recurso, AHORA.plusDays(6), AHORA.plusDays(7), "NO_SHOW");
        reserva(negocioA, recurso, AHORA.plusDays(8), AHORA.plusDays(9), "EXPIRADA");
        reserva(negocioA, recurso, AHORA.minusDays(4), AHORA.minusDays(2), "CONFIRMADA");

        assertThat(enContexto(negocioA, admin, VER,
                () -> reservas.delRecurso(recurso, AHORA, null, false))).isEmpty();
    }

    @Test
    @DisplayName("Criterio 4: una estancia en curso (check-in) que termina después de ahora cuenta")
    void estanciaEnCurso() {
        String enCasa = reserva(negocioA, recurso, AHORA.minusDays(1), AHORA.plusDays(1),
                "CHECK_IN");

        assertThat(enContexto(negocioA, admin, VER,
                () -> reservas.delRecurso(recurso, AHORA, null, false)))
                .extracting(ReservaDeUnRecurso::numero).containsExactly(enCasa);
    }

    @Test
    @DisplayName("Criterio 5: solo las confirmadas que se solapan con el periodo del bloqueo")
    void confirmadasQuePisaUnBloqueo() {
        OffsetDateTime desde = AHORA.plusDays(10);
        OffsetDateTime hasta = AHORA.plusDays(12);
        String pisada = reserva(negocioA, recurso, AHORA.plusDays(11), AHORA.plusDays(13),
                "CONFIRMADA");
        reserva(negocioA, recurso, AHORA.plusDays(9), AHORA.plusDays(10).minusHours(1),
                "CONFIRMADA");
        reserva(negocioA, recurso, AHORA.plusDays(10), AHORA.plusDays(11), "PENDIENTE");
        reserva(negocioA, UUID.randomUUID(), AHORA.plusDays(10), AHORA.plusDays(11),
                "CONFIRMADA");

        var afectadas = enContexto(negocioA, admin, VER,
                () -> reservas.delRecurso(recurso, desde, hasta, true));

        assertThat(afectadas).singleElement().satisfies(r -> {
            assertThat(r.numero()).isEqualTo(pisada);
            assertThat(r.estado()).isEqualTo("CONFIRMADA");
            assertThat(r.desde()).isEqualTo(AHORA.plusDays(11));
            assertThat(r.hasta()).isEqualTo(AHORA.plusDays(13));
        });
    }

    @Test
    @DisplayName("Criterio 9: el negocio B no ve las reservas del recurso del negocio A")
    void aislamientoEntreNegocios() {
        reserva(negocioA, recurso, AHORA.plusDays(3), AHORA.plusDays(5), "CONFIRMADA");
        String deB = reserva(negocioB, recurso, AHORA.plusDays(3), AHORA.plusDays(5),
                "CONFIRMADA");

        assertThat(enContexto(negocioA, admin, VER,
                () -> reservas.delRecurso(recurso, AHORA, null, false))).hasSize(1)
                .extracting(ReservaDeUnRecurso::numero).doesNotContain(deB);
        assertThat(enContexto(negocioB, admin, VER,
                () -> reservas.delRecurso(recurso, AHORA, null, false)))
                .extracting(ReservaDeUnRecurso::numero).containsExactly(deB);
    }

    @Test
    @DisplayName("Sin el permiso de ver reservas no se consulta, y un periodo invertido responde 422")
    void permisoYPeriodo() {
        assertThatThrownBy(() -> enContexto(negocioA, admin, Set.of(),
                () -> reservas.delRecurso(recurso, AHORA, null, false)))
                .isInstanceOf(SinPermisoException.class);
        assertThatThrownBy(() -> enContexto(negocioA, admin, VER,
                () -> reservas.delRecurso(recurso, AHORA, AHORA.minusDays(1), true)))
                .isInstanceOf(ReglaDeNegocioException.class);
    }
}
