package com.regenta.recursos.domain;

import java.math.BigDecimal;

/**
 * Lo que una política de cancelación decide para una reserva concreta (HU-068
 * criterio 3): cuánto anticipo hay que tener cobrado y cuánto se retiene si el
 * cliente cancela ahora.
 *
 * @param anticipoRequerido parte del total de la reserva que debe estar pagada
 * @param penalizacion      lo que se retiene por cancelar; 0 si se cancela dentro del plazo
 * @param horasDeAntelacion horas entre el momento de la cancelación y la entrada
 * @param dentroDePlazo     true si {@code horasDeAntelacion >= horasAntes} de la política
 */
public record ResultadoDeCancelacion(
        BigDecimal anticipoRequerido,
        BigDecimal penalizacion,
        long horasDeAntelacion,
        boolean dentroDePlazo) {
}
