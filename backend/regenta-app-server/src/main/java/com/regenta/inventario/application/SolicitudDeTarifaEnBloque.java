package com.regenta.inventario.application;

import java.util.List;
import java.util.UUID;

import com.regenta.inventario.domain.TarifaIva;

import jakarta.validation.constraints.NotNull;

/**
 * HU-154 · a qué productos se les fija la tarifa: los de {@code productoIds}, o, si viene vacío,
 * todos los de {@code categoriaId}. Fijar la misma tarifa que ya tienen solo la confirma (C2).
 */
public record SolicitudDeTarifaEnBloque(List<UUID> productoIds, UUID categoriaId,
        @NotNull TarifaIva tarifaIva) {
}
