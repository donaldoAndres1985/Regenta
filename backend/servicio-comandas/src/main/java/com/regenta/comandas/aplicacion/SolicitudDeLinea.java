package com.regenta.comandas.aplicacion;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Agregar una línea a la comanda (HU-085). */
public record SolicitudDeLinea(
        @NotNull UUID itemMenuId,
        @NotNull @Positive BigDecimal cantidad,
        List<UUID> modificadorIds,
        @Size(max = 200) String notas,
        Short comensalNumero,
        Integer secuenciaEnvio,
        String curso,
        BigDecimal descuentoValor,
        BigDecimal impuestoPct) {
}
