package com.regenta.compras.application;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Un pago a un proveedor contra una cuenta por pagar (HU-049 criterio 2). */
public record SolicitudDePago(
        @NotNull @Positive BigDecimal monto,
        String metodo,
        @Size(max = 60) String referencia,
        /**
         * HU-145 · Si el pago en efectivo sale de la caja, la sesión abierta de la que sale. Nula: se
         * pagó con dinero de fuera de la caja.
         */
        UUID sesionCajaId) {
}
