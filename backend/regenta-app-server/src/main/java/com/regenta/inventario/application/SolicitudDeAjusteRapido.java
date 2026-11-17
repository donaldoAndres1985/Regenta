package com.regenta.inventario.application;

import java.math.BigDecimal;
import java.util.UUID;

import com.regenta.inventario.domain.TipoAjuste;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * HU-159 · un ajuste de un producto en una bodega, hecho desde la app: sumar o restar una
 * cantidad, o fijarla por conteo físico. {@code tipo} dice por qué (avería, merma, conteo…) y
 * {@code motivo} lo explica; ambos quedan en el historial (I11).
 */
public record SolicitudDeAjusteRapido(
        @NotNull UUID productoId,
        @NotNull UUID bodegaId,
        @NotNull Operacion operacion,
        @NotNull @PositiveOrZero BigDecimal cantidad,
        @NotNull TipoAjuste tipo,
        @NotBlank @Size(max = 500) String motivo) {

    public enum Operacion { SUMAR, RESTAR, CONTEO }
}
