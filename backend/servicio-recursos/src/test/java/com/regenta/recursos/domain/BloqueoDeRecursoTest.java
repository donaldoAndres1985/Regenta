package com.regenta.recursos.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.comun.errores.ReglaDeNegocioException;

/** HU-067. Reglas puras de un bloqueo: el periodo tiene que tener sentido y saber si se solapa. */
class BloqueoDeRecursoTest {

    private static final UUID NEGOCIO = UUID.randomUUID();
    private static final UUID RECURSO = UUID.randomUUID();
    private static final OffsetDateTime T1 = OffsetDateTime.parse("2026-03-01T00:00:00Z");
    private static final OffsetDateTime T5 = OffsetDateTime.parse("2026-03-05T00:00:00Z");

    private static BloqueoDeRecurso bloqueo(OffsetDateTime desde, OffsetDateTime hasta) {
        return BloqueoDeRecurso.nuevo(NEGOCIO, RECURSO, desde, hasta, MotivoBloqueo.MANTENIMIENTO,
                null, null);
    }

    @Test
    @DisplayName("Un bloqueo sin inicio o sin fin se rechaza")
    void periodoIncompletoSeRechaza() {
        assertThatThrownBy(() -> bloqueo(null, T5)).isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> bloqueo(T1, null)).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("El fin del bloqueo debe ser posterior al inicio")
    void finAntesQueInicioSeRechaza() {
        assertThatThrownBy(() -> bloqueo(T5, T1)).isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> bloqueo(T1, T1)).isInstanceOf(ReglaDeNegocioException.class);
        assertThatCode(() -> bloqueo(T1, T5)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("solapaCon es cierto cuando los intervalos se pisan, falso cuando solo se tocan")
    void solapaConIntervaloMedioAbierto() {
        BloqueoDeRecurso b = bloqueo(T1, T5); // [mar 1, mar 5)

        assertThat(b.solapaCon(OffsetDateTime.parse("2026-03-03T00:00:00Z"),
                OffsetDateTime.parse("2026-03-08T00:00:00Z"))).isTrue();
        assertThat(b.solapaCon(OffsetDateTime.parse("2026-02-25T00:00:00Z"), T1)).isFalse();
        assertThat(b.solapaCon(T5, OffsetDateTime.parse("2026-03-08T00:00:00Z"))).isFalse();
        assertThat(b.solapaCon(OffsetDateTime.parse("2026-03-10T00:00:00Z"),
                OffsetDateTime.parse("2026-03-12T00:00:00Z"))).isFalse();
    }

    @Test
    @DisplayName("Sin motivo, el bloqueo queda como OTRO")
    void motivoPorDefecto() {
        BloqueoDeRecurso b = BloqueoDeRecurso.nuevo(NEGOCIO, RECURSO, T1, T5, null, "  ", null);

        assertThat(b.getMotivo()).isEqualTo(MotivoBloqueo.OTRO);
        assertThat(b.getDetalle()).isNull();
        assertThat(b.getId()).isNotNull();
    }

    @Test
    @DisplayName("Un motivo desconocido se rechaza")
    void motivoDesconocido() {
        assertThatThrownBy(() -> MotivoBloqueo.desde("REMODELACION"))
                .isInstanceOf(ReglaDeNegocioException.class);
    }
}
