package com.regenta.recursos.aplicacion;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Alta de un bloqueo de recurso (HU-067). El periodo es {@code [desde, hasta)}. */
public record SolicitudDeBloqueo(
        @NotNull OffsetDateTime desde,
        @NotNull OffsetDateTime hasta,
        String motivo,
        @Size(max = 2000) String detalle) {
}
