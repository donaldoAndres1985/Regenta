package com.regenta.ventas.aplicacion;

import java.math.BigDecimal;

/**
 * Lo que el POS necesita saber antes de cobrar (HU-137).
 *
 * @param montoIdentificarComprador sobre este total la venta no se cobra a
 *                                  consumidor final; {@code null} si el negocio
 *                                  no lo configuró
 */
public record ReglasDeCobro(BigDecimal montoIdentificarComprador) {
}
