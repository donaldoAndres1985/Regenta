package com.regenta.clientes.aplicacion;

import java.math.BigDecimal;

import jakarta.validation.constraints.PositiveOrZero;

/** Configuración de crédito del cliente (HU-022). */
public record SolicitudDeCredito(
        boolean habilitado,
        @PositiveOrZero BigDecimal cupo,
        @PositiveOrZero Integer diasCredito) {
}
