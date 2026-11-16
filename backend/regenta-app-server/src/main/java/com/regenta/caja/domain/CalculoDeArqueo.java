package com.regenta.caja.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.regenta.shared.web.ReglaDeNegocioException;

/**
 * HU-140 · el arqueo de cierre por método de pago, sin base de datos ni Spring.
 *
 * <ul>
 * <li>I7: el esperado de cada método es la suma con signo de sus movimientos.</li>
 * <li>Siempre hay línea de efectivo, aunque no haya movimientos en efectivo (I10 exige contarlo).</li>
 * <li>I9: en un método no cuadrable el contado no se pide: contado = esperado.</li>
 * <li>I9b: un cuadrable distinto de efectivo sin contado declarado cuenta como 0.</li>
 * <li>I9c: un cuadrable sin movimientos pero con contado declarado entra con esperado 0.</li>
 * <li>I10: el efectivo contado debe ser mayor que 0.</li>
 * </ul>
 */
public final class CalculoDeArqueo {

    static final String SIN_EFECTIVO = "Debe escribir la cantidad de efectivo que hay en caja.";

    private CalculoDeArqueo() {
    }

    public static List<LineaDeArqueo> calcular(List<EsperadoDeMetodo> esperados,
            Map<MetodoPagoCaja, BigDecimal> contados, ConfigDeCaja config) {
        BigDecimal efectivo = contados.get(MetodoPagoCaja.EFECTIVO);
        if (efectivo == null || efectivo.signum() <= 0) {
            throw new ReglaDeNegocioException(SIN_EFECTIVO);
        }

        Map<MetodoPagoCaja, EsperadoDeMetodo> porMetodo = new EnumMap<>(MetodoPagoCaja.class);
        esperados.forEach(e -> porMetodo.put(e.metodo(), e));
        porMetodo.putIfAbsent(MetodoPagoCaja.EFECTIVO,
                new EsperadoDeMetodo(MetodoPagoCaja.EFECTIVO, BigDecimal.ZERO, 0));
        contados.forEach((metodo, contado) -> {
            if (config.esCuadrable(metodo) && contado != null && contado.signum() != 0) {
                porMetodo.putIfAbsent(metodo, new EsperadoDeMetodo(metodo, BigDecimal.ZERO, 0));
            }
        });

        List<LineaDeArqueo> lineas = new ArrayList<>();
        for (EsperadoDeMetodo e : porMetodo.values()) {
            BigDecimal esperado = e.esperado() == null ? BigDecimal.ZERO : e.esperado();
            boolean cuadrable = config.esCuadrable(e.metodo());
            BigDecimal contado = cuadrable ? contados.getOrDefault(e.metodo(), BigDecimal.ZERO)
                    : esperado;
            lineas.add(new LineaDeArqueo(e.metodo(), cuadrable, esperado,
                    contado == null ? BigDecimal.ZERO : contado, e.movimientos()));
        }
        lineas.sort(Comparator.comparing(LineaDeArqueo::metodo));
        return lineas;
    }
}
