package com.regenta.ventas.aplicacion;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.regenta.ventas.domain.EstadoVenta;

public record VentaDelNegocio(
        UUID id,
        String numero,
        EstadoVenta estado,
        BigDecimal subtotal,
        BigDecimal descuentoTotal,
        BigDecimal baseGravable,
        BigDecimal impuestoTotal,
        BigDecimal total,
        BigDecimal costoTotal,
        List<LineaDeVenta> lineas) {
}
