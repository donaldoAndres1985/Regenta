package com.regenta.caja.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.shared.web.ReglaDeNegocioException;

/** HU-140 · el cálculo del arqueo por método de pago (invariantes I7, I9, I9b, I10). */
class CalculoDeArqueoTest {

    private final ConfigDeCaja config = ConfigDeCaja.porDefecto(UUID.randomUUID());

    private static BigDecimal $(long valor) {
        return BigDecimal.valueOf(valor);
    }

    private static LineaDeArqueo linea(List<LineaDeArqueo> lineas, MetodoPagoCaja metodo) {
        return lineas.stream().filter(l -> l.metodo() == metodo).findFirst().orElseThrow();
    }

    private static Map<MetodoPagoCaja, BigDecimal> contados(Object... pares) {
        Map<MetodoPagoCaja, BigDecimal> m = new EnumMap<>(MetodoPagoCaja.class);
        for (int i = 0; i < pares.length; i += 2) {
            m.put((MetodoPagoCaja) pares[i], (BigDecimal) pares[i + 1]);
        }
        return m;
    }

    @Test
    @DisplayName("HU-140 C1 · Dados movimientos en varios métodos, hay una línea por método con su esperado, sus movimientos y si es cuadrable")
    void unaLineaPorMetodoConMovimientos() {
        List<LineaDeArqueo> lineas = CalculoDeArqueo.calcular(List.of(
                new EsperadoDeMetodo(MetodoPagoCaja.EFECTIVO, $(150_000), 4),
                new EsperadoDeMetodo(MetodoPagoCaja.TRANSFERENCIA, $(80_000), 2),
                new EsperadoDeMetodo(MetodoPagoCaja.TARJETA_DEBITO, $(60_000), 1)),
                contados(MetodoPagoCaja.EFECTIVO, $(150_000), MetodoPagoCaja.TRANSFERENCIA, $(80_000)),
                config);

        assertThat(lineas).extracting(LineaDeArqueo::metodo).containsExactlyInAnyOrder(
                MetodoPagoCaja.EFECTIVO, MetodoPagoCaja.TRANSFERENCIA, MetodoPagoCaja.TARJETA_DEBITO);
        LineaDeArqueo transferencia = linea(lineas, MetodoPagoCaja.TRANSFERENCIA);
        assertThat(transferencia.esperado()).isEqualByComparingTo("80000");
        assertThat(transferencia.movimientos()).isEqualTo(2);
        assertThat(transferencia.cuadrable()).isTrue();
        assertThat(linea(lineas, MetodoPagoCaja.TARJETA_DEBITO).cuadrable()).isFalse();
    }

    @Test
    @DisplayName("HU-140 C2 · Dado un método no cuadrable, no se pide su contado: contado = esperado y diferencia = 0")
    void unMetodoNoCuadrableNoSeCuenta() {
        List<LineaDeArqueo> lineas = CalculoDeArqueo.calcular(List.of(
                new EsperadoDeMetodo(MetodoPagoCaja.EFECTIVO, $(10_000), 1),
                new EsperadoDeMetodo(MetodoPagoCaja.TARJETA_CREDITO, $(95_000), 3)),
                contados(MetodoPagoCaja.EFECTIVO, $(10_000), MetodoPagoCaja.TARJETA_CREDITO, $(1)),
                config);

        LineaDeArqueo tarjeta = linea(lineas, MetodoPagoCaja.TARJETA_CREDITO);
        assertThat(tarjeta.contado()).isEqualByComparingTo("95000");
        assertThat(tarjeta.diferencia()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("HU-140 C3 · Dado un método cuadrable distinto de efectivo sin contado, su contado es 0 y su diferencia es menos el esperado")
    void unCuadrableSinContadoCuentaComoCero() {
        List<LineaDeArqueo> lineas = CalculoDeArqueo.calcular(List.of(
                new EsperadoDeMetodo(MetodoPagoCaja.EFECTIVO, $(10_000), 1),
                new EsperadoDeMetodo(MetodoPagoCaja.QR, $(25_000), 2)),
                contados(MetodoPagoCaja.EFECTIVO, $(10_000)),
                config);

        LineaDeArqueo qr = linea(lineas, MetodoPagoCaja.QR);
        assertThat(qr.contado()).isEqualByComparingTo("0");
        assertThat(qr.diferencia()).isEqualByComparingTo("-25000");
    }

    @Test
    @DisplayName("HU-140 C4 · Dado un efectivo contado de 0 o sin declarar, el cierre se rechaza con 422")
    void sinEfectivoContadoNoSeCierra() {
        List<EsperadoDeMetodo> esperados =
                List.of(new EsperadoDeMetodo(MetodoPagoCaja.EFECTIVO, $(10_000), 1));

        assertThatThrownBy(() -> CalculoDeArqueo.calcular(esperados, contados(), config))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("Debe escribir la cantidad de efectivo que hay en caja.");
        assertThatThrownBy(() -> CalculoDeArqueo.calcular(esperados,
                contados(MetodoPagoCaja.EFECTIVO, BigDecimal.ZERO), config))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("HU-140 C1 · Dada una sesión sin movimientos en efectivo, la línea de efectivo aparece igual (I10 exige contarlo)")
    void elEfectivoSiempreTieneLinea() {
        List<LineaDeArqueo> lineas = CalculoDeArqueo.calcular(List.of(
                new EsperadoDeMetodo(MetodoPagoCaja.TRANSFERENCIA, $(5_000), 1)),
                contados(MetodoPagoCaja.EFECTIVO, $(2_000), MetodoPagoCaja.TRANSFERENCIA, $(5_000)),
                config);

        LineaDeArqueo efectivo = linea(lineas, MetodoPagoCaja.EFECTIVO);
        assertThat(efectivo.esperado()).isEqualByComparingTo("0");
        assertThat(efectivo.diferencia()).isEqualByComparingTo("2000");
    }
}
