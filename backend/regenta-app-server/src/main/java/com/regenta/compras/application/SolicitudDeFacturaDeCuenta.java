package com.regenta.compras.application;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** HU-170 C3 · Llegó la factura de una cuenta en espera: su número y su fecha. */
public record SolicitudDeFacturaDeCuenta(
        @NotBlank @Size(max = 40) String numeroFactura,
        @NotNull LocalDate fechaFactura) {
}
