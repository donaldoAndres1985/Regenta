package com.regenta.reportes.aplicacion;

import java.math.BigDecimal;

/**
 * Cómo se movieron las mesas en un rango (HU-099 criterio 2). La rotación es
 * cuántas sentadas dio en promedio cada mesa que se usó: 1,0 es una mesa que
 * se ocupó una vez en todo el día.
 */
public record MesasDelPeriodo(
        int comandas,
        int mesasUsadas,
        int comensales,
        BigDecimal rotacion,
        BigDecimal tiempoMedioMesaMin,
        BigDecimal ventaNeta,
        BigDecimal ticketPorComensal,
        BigDecimal ticketPorMesa) {
}
