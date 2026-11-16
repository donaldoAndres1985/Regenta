package com.regenta.caja.application;

import java.math.BigDecimal;
import java.util.Map;

import jakarta.validation.constraints.PositiveOrZero;

/**
 * Cierra la sesión declarando lo contado (HU-059, HU-140).
 *
 * <p>{@code contados}: lo contado por método de pago ({@code "EFECTIVO"}, {@code "QR"}…); los
 * no cuadrables se ignoran. Si no trae {@code EFECTIVO}, se toma {@code montoDeclarado} y, si
 * tampoco viene, el total del conteo por denominaciones (HU-062).
 */
public record SolicitudDeCierre(
        @PositiveOrZero BigDecimal montoDeclarado,
        String observaciones,
        Map<String, BigDecimal> contados) {
}
