package com.regenta.ventas.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

public record LineaDeVenta(
        short linea,
        UUID productoId,
        String sku,
        String nombre,
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        BigDecimal descuentoValor,
        BigDecimal impuestoPct,
        BigDecimal impuestoValor,
        BigDecimal baseGravable,
        BigDecimal subtotal,
        BigDecimal total) {
}
