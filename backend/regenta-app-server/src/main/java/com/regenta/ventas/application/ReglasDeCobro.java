package com.regenta.ventas.application;

import java.math.BigDecimal;

/**
 * Lo que el POS necesita saber antes de cobrar (HU-137).
 *
 * @param montoIdentificarComprador sobre este total la venta no se cobra a
 *                                  consumidor final; {@code null} si el negocio
 *                                  no lo configuró
 */
public record ReglasDeCobro(
        BigDecimal montoIdentificarComprador,
        boolean politicaStockNegativo,
        String razonSocial,
        String nombreComercial,
        String nit,
        String direccion,
        String telefono,
        String email,
        String ciudad) {

    public ReglasDeCobro(BigDecimal montoIdentificarComprador) {
        this(montoIdentificarComprador, false, null, null, null, null, null, null, null);
    }

    public ReglasDeCobro(BigDecimal montoIdentificarComprador, boolean politicaStockNegativo) {
        this(montoIdentificarComprador, politicaStockNegativo, null, null, null, null, null, null, null);
    }
}
