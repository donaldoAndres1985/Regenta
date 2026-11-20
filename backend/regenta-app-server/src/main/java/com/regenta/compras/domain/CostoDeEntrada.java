package com.regenta.compras.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * HU-168 · El costo con que cada producto de una factura entra al inventario (I14 de
 * {@code docs/dominio/compras.md}). Cálculo puro: no lee nada ni guarda nada.
 *
 * <p>Por línea, a 4 decimales:
 * <ol>
 * <li>{@code neto} = cantidad × costo × (1 − descuento de la línea).</li>
 * <li>El descuento global y el flete de la factura se reparten entre las líneas en proporción a su
 * neto (si todas valen cero, en proporción a la cantidad). El residuo del redondeo va a la última
 * línea, para que las partes sumen exactamente lo de la factura.</li>
 * <li>{@code base} = neto − descuento global de la línea; {@code iva} = base × tarifa.</li>
 * <li>{@code costo total} = base + flete de la línea, + IVA <b>solo si el negocio no es responsable
 * de IVA</b> (si lo es, el IVA es descontable y no entra al costo).</li>
 * <li>{@code costo unitario} = costo total ÷ cantidad. La última línea absorbe la diferencia del
 * redondeo, para que la suma de costo unitario × cantidad cuadre con la factura.</li>
 * </ol>
 */
public final class CostoDeEntrada {

    private static final int ESCALA = 4;
    private static final BigDecimal CIEN = new BigDecimal("100");

    private CostoDeEntrada() {
    }

    /** Una línea de la factura: cantidad, neto (con su descuento) y tarifa de IVA en porcentaje. */
    public record Entrada(BigDecimal cantidad, BigDecimal neto, BigDecimal impuestoPct) {
    }

    /** Lo que resulta para una línea. */
    public record Resultado(
            BigDecimal descuentoGlobal,
            BigDecimal flete,
            BigDecimal base,
            BigDecimal iva,
            BigDecimal costoTotal,
            BigDecimal costoUnitario) {
    }

    public static List<Resultado> calcular(List<Entrada> lineas, BigDecimal descuentoGlobal,
            BigDecimal flete, boolean responsableDeIva) {
        if (lineas.isEmpty()) {
            return List.of();
        }
        BigDecimal descuento = cero(descuentoGlobal);
        BigDecimal transporte = cero(flete);
        List<BigDecimal> pesos = pesosDe(lineas);
        List<BigDecimal> descuentos = repartir(descuento, pesos);
        List<BigDecimal> fletes = repartir(transporte, pesos);

        List<BigDecimal> totales = new ArrayList<>();
        List<Resultado> parciales = new ArrayList<>();
        for (int i = 0; i < lineas.size(); i++) {
            Entrada l = lineas.get(i);
            BigDecimal base = l.neto().subtract(descuentos.get(i)).setScale(ESCALA, RoundingMode.HALF_UP);
            BigDecimal iva = base.multiply(cero(l.impuestoPct()))
                    .divide(CIEN, ESCALA, RoundingMode.HALF_UP);
            BigDecimal total = base.add(fletes.get(i)).add(responsableDeIva ? BigDecimal.ZERO : iva);
            totales.add(total);
            parciales.add(new Resultado(descuentos.get(i), fletes.get(i), base, iva, total, null));
        }

        // Unitarios: la última línea absorbe lo que el redondeo deja suelto.
        List<Resultado> resultados = new ArrayList<>();
        BigDecimal granTotal = totales.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal acumulado = BigDecimal.ZERO;
        int ultima = lineas.size() - 1;
        for (int i = 0; i < lineas.size(); i++) {
            BigDecimal cantidad = lineas.get(i).cantidad();
            BigDecimal unitario = i < ultima
                    ? totales.get(i).divide(cantidad, ESCALA, RoundingMode.HALF_UP)
                    : granTotal.subtract(acumulado).divide(cantidad, ESCALA, RoundingMode.HALF_UP);
            acumulado = acumulado.add(unitario.multiply(cantidad));
            Resultado p = parciales.get(i);
            resultados.add(new Resultado(p.descuentoGlobal(), p.flete(), p.base(), p.iva(),
                    p.costoTotal(), unitario));
        }
        return resultados;
    }

    /** Por valor neto; si todo vale cero (regalos, muestras), por cantidad. */
    private static List<BigDecimal> pesosDe(List<Entrada> lineas) {
        boolean hayValor = lineas.stream().anyMatch(l -> l.neto().signum() > 0);
        return lineas.stream().map(l -> hayValor ? l.neto() : l.cantidad()).toList();
    }

    private static List<BigDecimal> repartir(BigDecimal monto, List<BigDecimal> pesos) {
        List<BigDecimal> partes = new ArrayList<>();
        BigDecimal suma = pesos.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal asignado = BigDecimal.ZERO;
        int ultima = pesos.size() - 1;
        for (int i = 0; i < pesos.size(); i++) {
            BigDecimal parte;
            if (monto.signum() == 0 || suma.signum() == 0) {
                parte = BigDecimal.ZERO.setScale(ESCALA);
            } else if (i < ultima) {
                parte = monto.multiply(pesos.get(i)).divide(suma, ESCALA, RoundingMode.HALF_UP);
            } else {
                parte = monto.subtract(asignado).setScale(ESCALA, RoundingMode.HALF_UP);
            }
            asignado = asignado.add(parte);
            partes.add(parte);
        }
        return partes;
    }

    private static BigDecimal cero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
