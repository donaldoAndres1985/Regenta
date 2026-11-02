package com.regenta.clientes.application;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Un pago contra una cuenta por cobrar. */
public record SolicitudDeRecaudo(
        @NotNull @Positive BigDecimal monto,
        @Size(max = 20) String metodo,
        @Size(max = 60) String referencia) {
}
