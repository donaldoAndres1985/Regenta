package com.regenta.recursos.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Cuánto suma un servicio adicional para una reserva concreta (HU-068 criterio
 * 1): las {@code unidades} salen del modo de cobro y el {@code subtotal} es
 * {@code precioUnitario × unidades}.
 */
public record CotizacionDeServicio(
        UUID servicioId,
        String nombre,
        String modoCobro,
        int personas,
        int noches,
        int unidades,
        BigDecimal precioUnitario,
        BigDecimal subtotal) {
}
