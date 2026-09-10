package com.regenta.comandas.aplicacion;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Abrir una comanda sobre una sesión de mesa (HU-085). */
public record SolicitudDeAperturaComanda(
        @NotNull UUID mesaId,
        @NotNull UUID sesionMesaId,
        @Positive Integer numComensales,
        String notas) {
}
