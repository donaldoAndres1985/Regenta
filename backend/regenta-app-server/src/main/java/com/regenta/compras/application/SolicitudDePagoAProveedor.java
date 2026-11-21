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
        @Valid List<Aplicacion> aplicaciones,
        /** HU-171 C5 · Usar el saldo a favor que el negocio tiene con este proveedor. */
        Boolean usarSaldoAFavor) {

    /** El pago de HU-171 sin saldo a favor. */
    public SolicitudDePagoAProveedor(UUID proveedorId, BigDecimal monto, String metodo, String referencia,
            Boolean pagarDesdeLaCaja, List<Aplicacion> aplicaciones) {
        this(proveedorId, monto, metodo, referencia, pagarDesdeLaCaja, aplicaciones, null);
    }

    public record Aplicacion(@NotNull UUID cuentaId, @NotNull @PositiveOrZero BigDecimal monto) {
    }
}
