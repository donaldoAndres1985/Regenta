package com.regenta.ventas.aplicacion;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Una venta que se registró sin señal y sube completa, de una sola vez
 * (HU-043). No se arma por pasos como la del mostrador: el celular ya la tiene
 * entera, y partirla en tres llamadas dejaría ventas a medias si la conexión se
 * vuelve a caer entre una y otra.
 *
 * @param origenOfflineId el id con que la creó el celular; es la clave de
 *        idempotencia de la subida (criterio 3)
 * @param ocurridoEn cuándo se hizo la venta de verdad, no cuándo subió
 */
public record SolicitudDeVentaOffline(
        @NotNull UUID origenOfflineId,
        @Size(max = 80) String dispositivoId,
        @NotNull OffsetDateTime ocurridoEn,
        @NotNull UUID bodegaId,
        UUID clienteId,
        UUID listaPreciosId,
        @Size(max = 20) String canal,
        @NotEmpty @Valid List<SolicitudDeLinea> lineas) {
}
