package com.regenta.shared.almacen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

/** HU-125 C5 · El almacén en /actuator/health: caído se ve, y no se le pregunta en cada consulta. */
class SaludDelAlmacenTest {

    @Test
    void c5_enMemoriaEstaArriba() {
        assertThat(new SaludDelAlmacen(new AlmacenEnMemoria()).health().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void c5_siElS3NoRespondeSeVeCaido() {
        AlmacenDeObjetos s3 = mock(AlmacenDeObjetos.class);
        doThrow(new IllegalStateException("sin red")).when(s3).comprobar();

        assertThat(new SaludDelAlmacen(s3).health().getStatus()).isEqualTo(Status.DOWN);
    }

    @Test
    void preguntaComoMuchoUnaVezPorMinuto() {
        AlmacenDeObjetos s3 = mock(AlmacenDeObjetos.class);
        Instant t0 = Instant.parse("2026-11-23T10:00:00Z");
        Clock[] reloj = {Clock.fixed(t0, ZoneOffset.UTC)};
        SaludDelAlmacen salud = new SaludDelAlmacen(s3, new Clock() {
            @Override
            public java.time.ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(java.time.ZoneId zona) {
                return this;
            }

            @Override
            public Instant instant() {
                return reloj[0].instant();
            }
        });

        salud.health();
        salud.health();
        reloj[0] = Clock.fixed(t0.plusSeconds(61), ZoneOffset.UTC);
        salud.health();

        verify(s3, times(2)).comprobar();
    }
}
