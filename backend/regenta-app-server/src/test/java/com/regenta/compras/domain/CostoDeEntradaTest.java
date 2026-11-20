package com.regenta.compras.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * HU-168 · Costo de entrada (I14). Factura de referencia: A, 10 und × $ 10.000; B, 5 und × $ 20.000;
 * ambas IVA_19; flete $ 10.000.
 */
class CostoDeEntradaTest {

    private static CostoDeEntrada.Entrada linea(String cantidad, String neto, String pct) {
        return new CostoDeEntrada.Entrada(new BigDecimal(cantidad), new BigDecimal(neto), new BigDecimal(pct));
    }

    private static final List<CostoDeEntrada.Entrada> REFERENCIA = List.of(
            linea("10", "100000", "19"), linea("5", "100000", "19"));

    private static BigDecimal cuadre(List<CostoDeEntrada.Entrada> lineas, List<CostoDeEntrada.Resultado> r) {
        BigDecimal suma = BigDecimal.ZERO;
        for (int i = 0; i < lineas.size(); i++) {
            suma = suma.add(r.get(i).costoUnitario().multiply(lineas.get(i).cantidad()));
        }
        return suma;
    }

    @Test
    @DisplayName("HU-168 C1 · Responsable de IVA: A entra a $ 10.500 y B a $ 21.000 (flete 50/50 por valor)")
    void responsable() {
        var r = CostoDeEntrada.calcular(REFERENCIA, null, new BigDecimal("10000"), true);

        assertThat(r.get(0).flete()).isEqualByComparingTo("5000");
        assertThat(r.get(1).flete()).isEqualByComparingTo("5000");
        assertThat(r.get(0).costoUnitario()).isEqualByComparingTo("10500");
        assertThat(r.get(1).costoUnitario()).isEqualByComparingTo("21000");
    }

    @Test
    @DisplayName("HU-168 C2 · No responsable: el IVA suma al costo; A entra a $ 12.400 y B a $ 24.800")
    void noResponsable() {
        var r = CostoDeEntrada.calcular(REFERENCIA, null, new BigDecimal("10000"), false);

        assertThat(r.get(0).costoUnitario()).isEqualByComparingTo("12400");
        assertThat(r.get(1).costoUnitario()).isEqualByComparingTo("24800");
        assertThat(cuadre(REFERENCIA, r)).isEqualByComparingTo("248000");
    }

    @Test
    @DisplayName("HU-168 C3 · Con 10 % de descuento en A, su neto y su parte del flete salen del precio rebajado")
    void descuentoDeLinea() {
        // A: 10 × 10.000 × 0,9 = 90.000; B: 100.000. Flete 10.000 → A 4.736,8421 y B 5.263,1579.
        var lineas = List.of(linea("10", "90000", "19"), linea("5", "100000", "19"));

        var r = CostoDeEntrada.calcular(lineas, null, new BigDecimal("10000"), true);

        assertThat(r.get(0).flete()).isEqualByComparingTo("4736.8421");
        assertThat(r.get(1).flete()).isEqualByComparingTo("5263.1579");
        assertThat(r.get(0).costoUnitario()).isEqualByComparingTo("9473.6842");
        assertThat(cuadre(lineas, r)).isEqualByComparingTo("200000");
    }

    @Test
    @DisplayName("HU-168 C4 · Un descuento global de $ 4.000 lo absorben A y B con $ 2.000 cada una")
    void descuentoGlobal() {
        var r = CostoDeEntrada.calcular(REFERENCIA, new BigDecimal("4000"), new BigDecimal("10000"), true);

        assertThat(r.get(0).descuentoGlobal()).isEqualByComparingTo("2000");
        assertThat(r.get(1).descuentoGlobal()).isEqualByComparingTo("2000");
        assertThat(r.get(0).base()).isEqualByComparingTo("98000");
        assertThat(r.get(0).iva()).isEqualByComparingTo("18620");
        // (98.000 + 5.000) / 10 = 10.300 ; (98.000 + 5.000) / 5 = 20.600
        assertThat(r.get(0).costoUnitario()).isEqualByComparingTo("10300");
        assertThat(r.get(1).costoUnitario()).isEqualByComparingTo("20600");
    }

    @Test
    @DisplayName("HU-168 C5 · Un reparto que no divide exacto deja el residuo en la última línea y la suma cuadra")
    void residuoALaUltima() {
        // Tres líneas iguales y $ 100 de flete: 33,3333 + 33,3333 + 33,3334.
        var lineas = List.of(linea("3", "1000", "0"), linea("3", "1000", "0"), linea("3", "1000", "0"));

        var r = CostoDeEntrada.calcular(lineas, null, new BigDecimal("100"), true);

        assertThat(r.get(0).flete()).isEqualByComparingTo("33.3333");
        assertThat(r.get(2).flete()).isEqualByComparingTo("33.3334");
        assertThat(r.stream().map(CostoDeEntrada.Resultado::flete).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("100");
        // Total 3.100: el redondeo de cada unitario se corrige en la última línea.
        assertThat(cuadre(lineas, r).subtract(new BigDecimal("3100")).abs())
                .isLessThan(new BigDecimal("0.001"));
    }

    @Test
    @DisplayName("HU-168 · Si todo vale cero (muestras), el flete se reparte por cantidad")
    void sinValorPorCantidad() {
        var lineas = List.of(linea("1", "0", "0"), linea("3", "0", "0"));

        var r = CostoDeEntrada.calcular(lineas, null, new BigDecimal("400"), true);

        assertThat(r.get(0).flete()).isEqualByComparingTo("100");
        assertThat(r.get(1).flete()).isEqualByComparingTo("300");
        assertThat(r.get(1).costoUnitario()).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("HU-168 · Sin flete ni descuento global, el costo de entrada es el costo neto (responsable)")
    void sinNadaQueRepartir() {
        var r = CostoDeEntrada.calcular(List.of(linea("4", "36000", "19")), null, null, true);

        assertThat(r.get(0).costoUnitario()).isEqualByComparingTo("9000");
    }
}
