package com.regenta.reportes.aplicacion;

import java.math.BigDecimal;

/** Lo que ve el dueño del negocio en el panel de inicio (HU-097). */
public record ResumenDiario(
        int documentos,
        BigDecimal unidades,
        BigDecimal montoBruto,
        BigDecimal descuentos,
        BigDecimal impuestos,
        BigDecimal montoNeto,
        BigDecimal costo,
        BigDecimal margen,
        BigDecimal ticketPromedio) {
}
