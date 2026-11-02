package com.regenta.reservas.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * El desglose de una estadÃ­a noche por noche que devuelve servicio-recursos
 * (HU-070 criterio 4). {@code completa} es false si alguna noche se quedÃ³ sin
 * tarifa.
 */
public record CotizacionDeEstadia(
        BigDecimal total,
        String moneda,
        boolean completa,
        List<NocheCotizada> noches) {

    public record NocheCotizada(LocalDate fecha, UUID tarifaId, BigDecimal precio) {
    }

    /** La tarifa de la primera noche, para dejarla como referencia en la cabecera de la reserva. */
    public UUID tarifaDeCabecera() {
        return noches == null || noches.isEmpty() ? null : noches.get(0).tarifaId();
    }
}

