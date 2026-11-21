package com.regenta.compras.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * HU-171 · Un pago a un proveedor que abona a una o varias de sus cuentas (I26). Sin
 * {@code aplicaciones}, se reparte de la cuenta más antigua a la más nueva.
 */
public record SolicitudDePagoAProveedor(
        @NotNull UUID proveedorId,
        @NotNull @Positive BigDecimal monto,
        String metodo,
        @Size(max = 60) String referencia,
        /** En efectivo con la plata de la caja abierta del usuario (HU-145). */
        Boolean pagarDesdeLaCaja,
        @Valid List<Aplicacion> aplicaciones) {

    public record Aplicacion(@NotNull UUID cuentaId, @NotNull @PositiveOrZero BigDecimal monto) {
    }
}
