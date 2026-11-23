package com.regenta.compras.application;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * HU-182 · I32 · La recepción que el celular armó (con o sin señal) y manda de una vez: lo que llegó,
 * con su factura, y los precios de venta nuevos con que se confirma. El identificador va en la ruta.
 */
public record SolicitudDeRecepcionDesdeElDispositivo(
        @NotNull @Valid SolicitudDeRecepcion recepcion,
        @Valid List<SolicitudDeConfirmacionDeRecepcion.PrecioNuevo> preciosNuevos) {
}
