package com.regenta.reservas.aplicacion;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * La cuenta de la estancia al salir (HU-074 criterio 1): alojamiento + servicios
 * + consumos, menos lo ya abonado. Al hacer el check-out se devuelve con el
 * estado ya en {@code CHECK_OUT} / {@code FINALIZADA}; como preview, con el
 * estado actual.
 */
public record LiquidacionDeEstancia(
        UUID reservaId,
        UUID estanciaId,
        String numeroReserva,
        BigDecimal alojamiento,
        BigDecimal servicios,
        BigDecimal consumos,
        BigDecimal subtotal,
        BigDecimal anticipo,
        BigDecimal saldoPendiente,
        String moneda,
        String estadoReserva,
        String estadoEstancia,
        OffsetDateTime checkOutEn) {
}
