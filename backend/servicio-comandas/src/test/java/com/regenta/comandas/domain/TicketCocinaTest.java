package com.regenta.comandas.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.comun.errores.ReglaDeNegocioException;

/** HU-088. Reglas puras del ticket de cocina: agrupa las líneas de una estación. */
class TicketCocinaTest {

    private static TicketCocina ticket() {
        return TicketCocina.crear(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1);
    }

    @Test
    @DisplayName("Nace NUEVO, sin marcas de tiempo de avance")
    void nace() {
        TicketCocina t = ticket();
        assertThat(t.getEstado()).isEqualTo(EstadoDeTicket.NUEVO);
        assertThat(t.getSecuencia()).isEqualTo(1);
        assertThat(t.getIniciadoEn()).isNull();
        assertThat(t.getListoEn()).isNull();
        assertThat(t.getEntregadoEn()).isNull();
        assertThat(t.getCreadoEn()).isNotNull();
    }

    @Test
    @DisplayName("El avance recorre NUEVO → EN_PREPARACION → LISTO → ENTREGADO y deja su marca de tiempo")
    void avanza() {
        TicketCocina t = ticket();
        UUID cocinero = UUID.randomUUID();

        t.avanzar(cocinero);
        assertThat(t.getEstado()).isEqualTo(EstadoDeTicket.EN_PREPARACION);
        assertThat(t.getIniciadoEn()).isNotNull();

        t.avanzar(cocinero);
        assertThat(t.getEstado()).isEqualTo(EstadoDeTicket.LISTO);
        assertThat(t.getListoEn()).isNotNull();

        t.avanzar(cocinero);
        assertThat(t.getEstado()).isEqualTo(EstadoDeTicket.ENTREGADO);
        assertThat(t.getEntregadoEn()).isNotNull();
        assertThat(t.getUsuarioCocinaId()).isEqualTo(cocinero);

        assertThatThrownBy(() -> t.avanzar(cocinero)).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Criterio 3: un ticket con más de 15 minutos se marca demorado")
    void demorado() {
        TicketCocina reciente = ticket();
        assertThat(reciente.demorado(reciente.getCreadoEn().plusMinutes(5))).isFalse();
        assertThat(reciente.demorado(reciente.getCreadoEn().plusMinutes(16))).isTrue();
    }

    @Test
    @DisplayName("Un ticket ENTREGADO ya no se marca demorado, aunque pasen los minutos")
    void demoradoNoAplicaEntregado() {
        TicketCocina t = ticket();
        OffsetDateTime creado = t.getCreadoEn();
        t.avanzar(UUID.randomUUID());
        t.avanzar(UUID.randomUUID());
        t.avanzar(UUID.randomUUID());
        assertThat(t.getEstado()).isEqualTo(EstadoDeTicket.ENTREGADO);
        assertThat(t.demorado(creado.plusMinutes(40))).isFalse();
    }

    @Test
    @DisplayName("Un tiempo objetivo propio reemplaza el umbral de 15 minutos por defecto")
    void tiempoObjetivoPropio() {
        TicketCocina t = ticket();
        t.fijarTiempoObjetivo((short) 5);
        assertThat(t.demorado(t.getCreadoEn().plusMinutes(6))).isTrue();
        assertThat(t.demorado(t.getCreadoEn().plusMinutes(4))).isFalse();
    }
}
