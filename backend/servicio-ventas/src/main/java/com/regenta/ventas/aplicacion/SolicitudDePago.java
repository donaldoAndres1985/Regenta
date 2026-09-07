package com.regenta.ventas.aplicacion;

import java.math.BigDecimal;

import com.regenta.ventas.domain.MetodoDePago;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Un pago a registrar. Para {@code EFECTIVO} va {@code montoRecibido} (lo que
 * entregó el cliente) y el cambio se calcula; para el resto va {@code monto}.
 */
public record SolicitudDePago(
        @NotNull MetodoDePago metodo,
        @Positive BigDecimal monto,
        @Positive BigDecimal montoRecibido,
        @Size(max = 80) String referencia,
        @Size(max = 30) String franquicia,
        Integer diasCredito) {
}
