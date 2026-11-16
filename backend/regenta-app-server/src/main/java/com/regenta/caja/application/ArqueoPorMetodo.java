package com.regenta.caja.application;

import java.math.BigDecimal;
import java.util.List;

import com.regenta.caja.domain.LineaDeArqueo;

/**
 * HU-140 · el arqueo de una sesión por método de pago, con sus totales.
 *
 * <p>Con la sesión abierta, {@code contado} y {@code diferencia} de un método cuadrable pueden
 * venir nulos: todavía no se contó. Los totales suman solo lo que tiene contado.
 */
public record ArqueoPorMetodo(List<Linea> lineas, BigDecimal totalEsperado,
        BigDecimal totalContado, BigDecimal totalDiferencia) {

    public ArqueoPorMetodo(List<Linea> lineas) {
        this(lineas, suma(lineas, Linea::esperado), suma(lineas, Linea::contado),
                suma(lineas, Linea::diferencia));
    }

    static ArqueoPorMetodo de(List<LineaDeArqueo> guardadas) {
        return new ArqueoPorMetodo(guardadas.stream().map(Linea::de).toList());
    }

    private static BigDecimal suma(List<Linea> lineas,
            java.util.function.Function<Linea, BigDecimal> campo) {
        return lineas.stream().map(campo).filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public record Linea(String metodo, boolean cuadrable, BigDecimal esperado, BigDecimal contado,
            BigDecimal diferencia, long movimientos) {

        static Linea de(LineaDeArqueo l) {
            return new Linea(l.metodo().name(), l.cuadrable(), l.esperado(), l.contado(),
                    l.diferencia(), l.movimientos());
        }
    }
}
