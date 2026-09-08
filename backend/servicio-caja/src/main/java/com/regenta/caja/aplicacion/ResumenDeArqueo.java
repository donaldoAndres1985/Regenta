package com.regenta.caja.aplicacion;

import java.math.BigDecimal;
import java.util.List;

import com.regenta.caja.domain.ArqueoDenominacion;

/** El arqueo con su total y la diferencia contra lo esperado (HU-062 criterios 1 y 2). */
public record ResumenDeArqueo(
        BigDecimal totalContado,
        BigDecimal montoEsperado,
        BigDecimal diferencia,
        List<LineaContada> denominaciones) {

    public record LineaContada(BigDecimal denominacion, String tipo, int cantidad,
            BigDecimal subtotal) {
        static LineaContada de(ArqueoDenominacion a) {
            return new LineaContada(a.getDenominacion(), a.getTipo().name(), a.getCantidad(),
                    a.getSubtotal());
        }
    }
}
