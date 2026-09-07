package com.regenta.ventas.aplicacion;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

/** Alta de una cotización. {@code validaHasta} opcional. */
public record SolicitudDeCotizacion(
        UUID clienteId,
        LocalDate validaHasta,
        @NotEmpty @Valid List<LineaDeCotizacion> lineas) {
}
