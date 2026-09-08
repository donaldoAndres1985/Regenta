package com.regenta.recursos.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

import com.regenta.recursos.domain.PoliticaCancelacion;
import com.regenta.recursos.domain.ResultadoDeCancelacion;

/**
 * Lo que una política resuelve para una reserva (HU-068 criterio 3): el anticipo
 * que hay que exigir y la penalización si se cancela ahora.
 */
public record AplicacionDeCancelacion(
        UUID politicaId,
        String politicaNombre,
        int horasAntes,
        BigDecimal montoReserva,
        BigDecimal anticipoRequerido,
        BigDecimal penalizacion,
        long horasDeAntelacion,
        boolean dentroDePlazo) {

    static AplicacionDeCancelacion de(PoliticaCancelacion politica, BigDecimal montoReserva,
            ResultadoDeCancelacion resultado) {
        return new AplicacionDeCancelacion(politica.getId(), politica.getNombre(),
                politica.getHorasAntes(), montoReserva, resultado.anticipoRequerido(),
                resultado.penalizacion(), resultado.horasDeAntelacion(), resultado.dentroDePlazo());
    }
}
