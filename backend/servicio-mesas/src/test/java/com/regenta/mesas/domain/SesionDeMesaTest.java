package com.regenta.mesas.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.comun.errores.ReglaDeNegocioException;

/** HU-082. Reglas puras de una sesión de mesa. */
class SesionDeMesaTest {

    private static SesionDeMesa abierta(int comensales) {
        return SesionDeMesa.abrir(UUID.randomUUID(), UUID.randomUUID(), comensales,
                UUID.randomUUID());
    }

    @Test
    @DisplayName("Criterio 1: se abre ABIERTA, con comensales y con la hora de inicio")
    void abrir() {
        SesionDeMesa s = abierta(4);
        assertThat(s.getEstado()).isEqualTo(EstadoDeSesion.ABIERTA);
        assertThat(s.getNumComensales()).isEqualTo((short) 4);
        assertThat(s.getAbiertaEn()).isNotNull();
        assertThat(s.getCerradaEn()).isNull();
        assertThat(s.estaViva()).isTrue();
        assertThat(s.minutosTranscurridos()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Una sesión necesita al menos un comensal")
    void comensales() {
        assertThatThrownBy(() -> abierta(0)).isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> abierta(-2)).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Pedir la cuenta pasa de ABIERTA a CUENTA_PEDIDA; no desde otro estado")
    void pedirCuenta() {
        SesionDeMesa s = abierta(2);
        s.pedirCuenta();
        assertThat(s.getEstado()).isEqualTo(EstadoDeSesion.CUENTA_PEDIDA);
        assertThat(s.estaViva()).isTrue();

        s.cerrar(OffsetDateTime.now());
        assertThatThrownBy(s::pedirCuenta).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Criterio 5: al cerrar queda CERRADA con su fin; cerrar otra vez no cambia nada")
    void cerrar() {
        SesionDeMesa s = abierta(3);
        OffsetDateTime fin = OffsetDateTime.now();
        s.cerrar(fin);
        assertThat(s.getEstado()).isEqualTo(EstadoDeSesion.CERRADA);
        assertThat(s.getCerradaEn()).isEqualTo(fin);
        assertThat(s.estaViva()).isFalse();

        s.cerrar(fin.plusMinutes(30)); // idempotente
        assertThat(s.getCerradaEn()).isEqualTo(fin);
    }

    @Test
    @DisplayName("Anular una sesión ya cerrada se rechaza")
    void anular() {
        SesionDeMesa s = abierta(1);
        s.cerrar(OffsetDateTime.now());
        assertThatThrownBy(s::anular).isInstanceOf(ReglaDeNegocioException.class);
    }
}
