package com.regenta.reportes.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Un día de hotel (HU-099 criterio 1). {@code adr} es lo que rindió en
 * promedio cada habitación vendida; {@code revpar}, lo que rindió cada
 * habitación que había, vendida o no.
 */
public record OcupacionDelDia(
        LocalDate fecha,
        UUID tipoRecursoId,
        String tipoRecurso,
        int recursosTotales,
        int recursosOcupados,
        BigDecimal ocupacionPct,
        BigDecimal ingresoAlojamiento,
        BigDecimal adr,
        BigDecimal revpar) {
}
