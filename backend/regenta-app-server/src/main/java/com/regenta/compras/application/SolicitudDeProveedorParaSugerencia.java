package com.regenta.compras.application;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * HU-181 C5 · El proveedor que se le asigna a un producto que salió en «Sin proveedor». Queda como su
 * preferido en el catálogo, y sus sugerencias pendientes pasan a ese grupo.
 */
public record SolicitudDeProveedorParaSugerencia(@NotNull(message = "Elige el proveedor.") UUID proveedorId) {
}
