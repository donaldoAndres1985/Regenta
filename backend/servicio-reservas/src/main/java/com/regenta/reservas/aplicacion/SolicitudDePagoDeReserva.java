package com.regenta.reservas.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Registrar un pago sobre una reserva (HU-071): anticipo, saldo, depósito… */
public record SolicitudDePagoDeReserva(
        @NotBlank String tipo,
        String metodo,
        @NotNull @Positive BigDecimal monto,
        String referencia,
        UUID cajaSesionId) {
}
