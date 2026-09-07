package com.regenta.ventas.aplicacion;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Alta de una venta en borrador. El cliente es opcional (consumidor final). */
public record SolicitudDeVenta(
        @NotNull UUID bodegaId,
        UUID clienteId,
        UUID listaPreciosId,
        @Size(max = 20) String canal) {
}
