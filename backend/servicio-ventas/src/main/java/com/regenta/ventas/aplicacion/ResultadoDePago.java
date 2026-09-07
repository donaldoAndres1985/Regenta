package com.regenta.ventas.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

import com.regenta.ventas.domain.MetodoDePago;

public record ResultadoDePago(
        UUID pagoId,
        MetodoDePago metodo,
        BigDecimal montoAplicado,
        BigDecimal cambio,
        BigDecimal saldoPendiente,
        boolean cubierta) {
}
