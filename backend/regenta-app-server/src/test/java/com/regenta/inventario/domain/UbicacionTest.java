package com.regenta.inventario.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.shared.web.ReglaDeNegocioException;

/** HU-152 · I9: la ubicación es opcional, por producto y bodega, hasta 60 caracteres. */
class UbicacionTest {

    private final Existencia existencia =
            Existencia.enCero(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

    @Test
    @DisplayName("HU-152 C1 · Se guarda la ubicación escrita, sin espacios sobrantes")
    void seGuarda() {
        existencia.fijarUbicacion("  Pasillo 3 – E2 ");

        assertThat(existencia.getUbicacion()).isEqualTo("Pasillo 3 – E2");
    }

    @Test
    @DisplayName("HU-152 C3 · Una ubicación de más de 60 caracteres se rechaza")
    void masDe60SeRechaza() {
        assertThatThrownBy(() -> existencia.fijarUbicacion("x".repeat(61)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("60");
    }

    @Test
    @DisplayName("HU-152 · Dejarla en blanco la quita")
    void enBlancoLaQuita() {
        existencia.fijarUbicacion("Estiba 5");
        existencia.fijarUbicacion("   ");

        assertThat(existencia.getUbicacion()).isNull();
    }
}
