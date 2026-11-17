package com.regenta.inventario.application;

import java.math.BigDecimal;
import java.util.UUID;

/** HU-161 · "1 {@code origen} = {@code factor} {@code destino}". */
public record EquivalenciaDelNegocio(UUID id, UUID origenId, String origen, UUID destinoId,
        String destino, BigDecimal factor) {
}
