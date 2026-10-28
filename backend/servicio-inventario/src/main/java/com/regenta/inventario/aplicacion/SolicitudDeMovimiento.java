package com.regenta.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

import com.regenta.inventario.domain.OrigenMovimiento;
import com.regenta.inventario.domain.TipoMovimiento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Un movimiento a anotar en el libro. La {@code idempotencyKey} es lo que evita
 * el doble descuento cuando un evento se entrega dos veces.
 *
 * <p>{@code codigoLote} y {@code autorizacionVencido} solo importan cuando el
 * producto maneja lotes (HU-031): en una entrada el codigo es obligatorio; en
 * una salida de un lote vencido hace falta la autorizacion.
 *
 * <p>{@code costoUnitario} solo importa en una entrada: si viene, pasa a ser el
 * ultimo costo del producto (HU-138, criterio 6).
 */
public record SolicitudDeMovimiento(
        @NotNull UUID productoId,
        @NotNull UUID bodegaId,
        @NotNull TipoMovimiento tipo,
        @NotNull @Positive BigDecimal cantidad,
        @NotNull OrigenMovimiento origenTipo,
        UUID origenId,
        String motivo,
        @NotBlank String idempotencyKey,
        String codigoLote,
        String autorizacionVencido,
        @PositiveOrZero BigDecimal costoUnitario) {

    /** Movimiento sin lote: el caso de un producto que no maneja lotes. */
    public SolicitudDeMovimiento(UUID productoId, UUID bodegaId, TipoMovimiento tipo,
            BigDecimal cantidad, OrigenMovimiento origenTipo, UUID origenId, String motivo,
            String idempotencyKey) {
        this(productoId, bodegaId, tipo, cantidad, origenTipo, origenId, motivo, idempotencyKey,
                null, null, null);
    }

    /** Movimiento sin costo. */
    public SolicitudDeMovimiento(UUID productoId, UUID bodegaId, TipoMovimiento tipo,
            BigDecimal cantidad, OrigenMovimiento origenTipo, UUID origenId, String motivo,
            String idempotencyKey, String codigoLote, String autorizacionVencido) {
        this(productoId, bodegaId, tipo, cantidad, origenTipo, origenId, motivo, idempotencyKey,
                codigoLote, autorizacionVencido, null);
    }
}
