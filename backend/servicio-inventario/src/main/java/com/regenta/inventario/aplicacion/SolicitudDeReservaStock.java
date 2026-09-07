package com.regenta.inventario.aplicacion;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Lo que pide la saga de ventas para apartar stock. El {@code origenTipo} +
 * {@code origenId} identifican la venta; {@code correlacionId} es el id de la
 * saga. {@code expiraEn} opcional: si no viene, se usa el TTL por defecto.
 */
public record SolicitudDeReservaStock(
        @NotBlank @Size(max = 20) String origenTipo,
        @NotNull UUID origenId,
        @NotNull UUID correlacionId,
        OffsetDateTime expiraEn,
        @NotEmpty @Valid List<LineaDeReserva> lineas) {
}
