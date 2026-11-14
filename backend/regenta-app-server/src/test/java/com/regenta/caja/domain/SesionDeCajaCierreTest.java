package com.regenta.caja.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.shared.web.ConflictoDeEstadoException;

/** HU-140 · el cierre de la sesión con el arqueo por método (invariantes I11, I12). */
class SesionDeCajaCierreTest {

    private final SesionDeCaja sesion = SesionDeCaja.abrir(UUID.randomUUID(), null,
            UUID.randomUUID(), "CAJA-1", UUID.randomUUID(), new BigDecimal("50000"));

    private static LineaDeArqueo linea(MetodoPagoCaja metodo, boolean cuadrable, long esperado,
            long contado) {
        return new LineaDeArqueo(metodo, cuadrable, BigDecimal.valueOf(esperado),
                BigDecimal.valueOf(contado), 1);
    }

    @Test
    @DisplayName("HU-140 C7 · Dadas todas las diferencias en 0, cuando cierro, la sesión queda CUADRADA")
    void todoCuadraEntoncesCuadrada() {
        sesion.cerrarConArqueo(UUID.randomUUID(), List.of(
                linea(MetodoPagoCaja.EFECTIVO, true, 120_000, 120_000),
                linea(MetodoPagoCaja.TARJETA_DEBITO, false, 40_000, 40_000)), null);

        assertThat(sesion.getEstado()).isEqualTo(EstadoSesionCaja.CUADRADA);
        assertThat(sesion.getMontoEsperado()).isEqualByComparingTo("120000");
        assertThat(sesion.getMontoDeclarado()).isEqualByComparingTo("120000");
    }

    @Test
    @DisplayName("HU-140 C8 · Dada una diferencia distinta de 0 en un método que no es efectivo, cuando cierro, la sesión queda DESCUADRADA")
    void unaLineaDescuadradaDescuadraLaSesion() {
        sesion.cerrarConArqueo(UUID.randomUUID(), List.of(
                linea(MetodoPagoCaja.EFECTIVO, true, 120_000, 120_000),
                linea(MetodoPagoCaja.TRANSFERENCIA, true, 30_000, 20_000)), null);

        assertThat(sesion.getEstado()).isEqualTo(EstadoSesionCaja.DESCUADRADA);
        assertThat(sesion.quedoDescuadrada()).isTrue();
    }

    @Test
    @DisplayName("HU-140 C11 · Dada una sesión cerrada, cuando intento cerrarla otra vez, se rechaza con 409")
    void unaSesionCerradaNoSeVuelveACerrar() {
        List<LineaDeArqueo> lineas = List.of(linea(MetodoPagoCaja.EFECTIVO, true, 50_000, 50_000));
        sesion.cerrarConArqueo(UUID.randomUUID(), lineas, null);

        assertThatThrownBy(() -> sesion.cerrarConArqueo(UUID.randomUUID(), lineas, null))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }
}
