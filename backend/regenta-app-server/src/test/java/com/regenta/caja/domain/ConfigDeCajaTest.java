package com.regenta.caja.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumSet;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.shared.web.ReglaDeNegocioException;

/** HU-139 · reglas puras del agregado {@link ConfigDeCaja} (invariante I8 de caja). */
class ConfigDeCajaTest {

    @Test
    @DisplayName("HU-139 C1 · Dado un negocio que nunca configuró caja, los métodos no cuadrables son TARJETA_DEBITO, TARJETA_CREDITO, BONO y CREDITO")
    void porDefectoNoSeCuadranTarjetasBonoYCredito() {
        ConfigDeCaja config = ConfigDeCaja.porDefecto(UUID.randomUUID());

        assertThat(config.getMetodosNoCuadrables()).containsExactlyInAnyOrder(
                MetodoPagoCaja.TARJETA_DEBITO, MetodoPagoCaja.TARJETA_CREDITO,
                MetodoPagoCaja.BONO, MetodoPagoCaja.CREDITO);
    }

    @Test
    @DisplayName("HU-139 C2 · Dado un cambio de la lista, la configuración devuelve la lista nueva")
    void cambiarLaListaLaReemplaza() {
        ConfigDeCaja config = ConfigDeCaja.porDefecto(UUID.randomUUID());

        config.fijarMetodosNoCuadrables(EnumSet.of(MetodoPagoCaja.BONO, MetodoPagoCaja.QR));

        assertThat(config.getMetodosNoCuadrables())
                .containsExactlyInAnyOrder(MetodoPagoCaja.BONO, MetodoPagoCaja.QR);
    }

    @Test
    @DisplayName("HU-139 C4 · Dada una lista que incluye EFECTIVO, se rechaza: el efectivo siempre se cuadra")
    void elEfectivoNoPuedeQuedarComoNoCuadrable() {
        ConfigDeCaja config = ConfigDeCaja.porDefecto(UUID.randomUUID());

        assertThatThrownBy(() -> config.fijarMetodosNoCuadrables(
                EnumSet.of(MetodoPagoCaja.EFECTIVO, MetodoPagoCaja.BONO)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("El efectivo siempre se cuadra");
        assertThat(config.getMetodosNoCuadrables()).doesNotContain(MetodoPagoCaja.EFECTIVO);
    }
}
