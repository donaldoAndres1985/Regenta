package com.regenta.reportes.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Un día de hotel (HU-099 criterio 1). {@code adr} es lo que rindió en
 * promedio cada habitación vendida; {@code revpar}, lo que rindió cada
 * habitación que había, vendida o no.
 *
 * <p>{@code recursosTotales} ya viene neto de bloqueos (HU-136): es lo que
 * había <b>para vender</b> esa noche. {@code recursosBloqueados} y
 * {@code motivosBloqueo} están para poder explicar esa resta — un denominador
 * que baja sin explicación parece un error de datos.
 */
public record OcupacionDelDia(
        LocalDate fecha,
        UUID tipoRecursoId,
        String tipoRecurso,
        int recursosTotales,
        int recursosOcupados,
        int recursosBloqueados,
        Map<String, Integer> motivosBloqueo,
        BigDecimal ocupacionPct,
        BigDecimal ingresoAlojamiento,
        BigDecimal adr,
        BigDecimal revpar) {
}
