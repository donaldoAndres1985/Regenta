package com.regenta.menu.api;

import java.math.BigDecimal;
import java.util.UUID;

public record ModificadorElegidoDto(
        UUID id,
        String nombre,
        BigDecimal precioExtra) {
}
