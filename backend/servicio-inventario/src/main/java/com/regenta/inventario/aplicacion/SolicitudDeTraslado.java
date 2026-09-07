package com.regenta.inventario.aplicacion;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Alta de un traslado en BORRADOR, con sus lineas. */
public record SolicitudDeTraslado(
        @NotBlank @Size(max = 30) String numero,
        @NotNull UUID bodegaOrigenId,
        @NotNull UUID bodegaDestinoId,
        @Size(max = 2000) String observaciones,
        @NotEmpty @Valid List<LineaDeTraslado> lineas) {
}
