package com.regenta.caja.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Un retiro o gasto de efectivo (HU-061 criterios 1 y 2). Sobre el umbral
 * configurado, `autorizadoPor` es obligatorio.
 */
public record SolicitudDeRetiro(
        @NotNull @Positive BigDecimal monto,
        @NotBlank @Size(max = 200) String concepto,
        String tipo,
        UUID autorizadoPor) {
}
