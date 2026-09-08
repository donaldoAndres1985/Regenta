package com.regenta.recursos.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** El desglose de una estancia noche por noche, cada una a su tarifa (HU-066). */
public record CotizacionDeEstadia(
        UUID recursoId,
        int personas,
        BigDecimal total,
        String moneda,
        boolean completa,
        List<NocheCotizada> noches) {

    public record NocheCotizada(
            LocalDate fecha,
            UUID tarifaId,
            String tarifaNombre,
            int prioridad,
            BigDecimal precio) {
    }
}
