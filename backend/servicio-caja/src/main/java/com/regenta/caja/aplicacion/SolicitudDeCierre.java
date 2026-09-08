package com.regenta.caja.aplicacion;

import java.math.BigDecimal;

import jakarta.validation.constraints.PositiveOrZero;

/**
 * Cierra la sesión declarando lo contado (HU-059). El monto declarado puede
 * venir explícito o salir del arqueo por denominaciones guardado (HU-062): si
 * {@code montoDeclarado} es nulo, se toma el total del arqueo.
 */
public record SolicitudDeCierre(
        @PositiveOrZero BigDecimal montoDeclarado,
        String observaciones) {
}
