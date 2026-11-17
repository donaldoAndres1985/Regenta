package com.regenta.ventas.application;

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
        /** HU-148: IVA_19, IVA_5, EXENTO, EXCLUIDO o NO_RESPONSABLE. */
        String impuestoCodigo,
        BigDecimal impuestoValor,
        BigDecimal baseGravable,
        BigDecimal subtotal,
        BigDecimal total) {
}
