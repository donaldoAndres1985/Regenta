package com.regenta.inventario.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * HU-161 C3 · replicar una equivalencia a varios productos: cada origen se une al producto
 * cuyo nombre es el suyo cambiando {@code buscar} por {@code reemplazar} (sin tildes ni
 * mayúsculas): «Pintulux rojo cuñete» → «Pintulux rojo galón».
 */
public record SolicitudDeReplica(
        @NotEmpty List<UUID> origenIds,
        @NotBlank String buscar,
        @NotBlank String reemplazar,
        @NotNull BigDecimal factor) {
}
