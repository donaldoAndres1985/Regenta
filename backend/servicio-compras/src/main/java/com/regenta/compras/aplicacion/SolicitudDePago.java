package com.regenta.compras.aplicacion;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Un pago a un proveedor contra una cuenta por pagar (HU-049 criterio 2). */
public record SolicitudDePago(
        @NotNull @Positive BigDecimal monto,
        String metodo,
        @Size(max = 60) String referencia) {
}
