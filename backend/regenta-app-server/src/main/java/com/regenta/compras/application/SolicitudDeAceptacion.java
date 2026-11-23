package com.regenta.compras.application;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * Acepta el grupo de un proveedor: se crea una orden de compra en borrador con
 * esas sugerencias (HU-050 criterio 3). Si no se listan ids, entran todas las
 * sugerencias pendientes de ese proveedor; con {@code bodegaDestinoId}, solo las de esa bodega
 * (HU-181). Si son de varias bodegas y no se dice cuál, se pide.
 */
public record SolicitudDeAceptacion(
        @NotNull UUID proveedorId,
        UUID bodegaDestinoId,
        List<UUID> sugerenciaIds) {
}
