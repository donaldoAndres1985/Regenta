package com.regenta.compras.application;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Registra la factura del proveedor sobre una recepción confirmada (HU-049 C1, HU-170 C3). Si la
 * recepción llegó con remisión, completa su cuenta en espera de factura; si no tiene cuenta, la abre.
 * Sin fecha, se toma la de hoy.
 */
public record SolicitudDeFacturaDeRecepcion(
        @NotNull UUID recepcionId,
        @NotBlank @Size(max = 40) String numeroFactura,
        LocalDate fechaFactura) {

    public SolicitudDeFacturaDeRecepcion(UUID recepcionId, String numeroFactura) {
        this(recepcionId, numeroFactura, null);
    }
}
