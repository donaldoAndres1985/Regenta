package com.regenta.inventario.application;

import com.regenta.inventario.api.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.regenta.inventario.domain.EstadoTraslado;

public record TrasladoDelNegocio(
        UUID id,
        String numero,
        UUID bodegaOrigenId,
        UUID bodegaDestinoId,
        EstadoTraslado estado,
        OffsetDateTime fechaEnvio,
        OffsetDateTime fechaRecepcion,
        List<LineaDelTraslado> lineas) {
}
