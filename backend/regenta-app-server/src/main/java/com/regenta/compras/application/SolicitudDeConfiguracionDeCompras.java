package com.regenta.compras.application;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;

/** HU-173 · Si las órdenes exigen aprobación, desde qué monto, y la tolerancia de recepción (%). */
public record SolicitudDeConfiguracionDeCompras(
        boolean exigeAprobacion,
        @Positive BigDecimal montoMinimoAprobacion,
        @DecimalMin("0") @DecimalMax("100") BigDecimal toleranciaRecepcionPct) {
}
