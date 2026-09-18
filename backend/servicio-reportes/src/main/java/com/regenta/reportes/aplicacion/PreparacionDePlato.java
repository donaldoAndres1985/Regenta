package com.regenta.reportes.aplicacion;

import java.math.BigDecimal;

/**
 * Cuánto se demora la cocina en sacar un plato (HU-099 criterio 4). Sale de
 * las marcas del KDS —enviada y lista—, no de una estimación de la carta.
 */
public record PreparacionDePlato(
        String plato,
        int veces,
        BigDecimal minutosPromedio,
        Integer minutosMaximo) {
}
