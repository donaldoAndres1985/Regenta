package com.regenta.inventario.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * HU-165 · Lo que llegó en una recepción de Compras, tal como Inventario lo necesita para darle
 * entrada. Se arma desde el evento {@code recepcion_registrada}.
 */
public record EntradaDeRecepcion(
        UUID recepcionId,
        String numero,
        UUID bodegaId,
        List<Linea> lineas) {

    public record Linea(
            UUID productoId,
            BigDecimal cantidad,
            BigDecimal costoUnitario,
            String codigoLote,
            LocalDate fechaVencimiento,
            String registroSanitario) {
    }
}
