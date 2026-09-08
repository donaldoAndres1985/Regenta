package com.regenta.compras.aplicacion;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Registra la factura del proveedor sobre una recepción confirmada (HU-049 criterio 1). */
public record SolicitudDeFacturaDeRecepcion(
        @NotNull UUID recepcionId,
        @NotBlank @Size(max = 40) String numeroFactura) {
}
