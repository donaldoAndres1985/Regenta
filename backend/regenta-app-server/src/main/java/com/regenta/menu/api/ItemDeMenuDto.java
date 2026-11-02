package com.regenta.menu.api;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemDeMenuDto(
        UUID id,
        String nombre,
        BigDecimal precio,
        UUID estacionId,
        String curso,
        BigDecimal costoEstimado,
        boolean disponible) {
}
