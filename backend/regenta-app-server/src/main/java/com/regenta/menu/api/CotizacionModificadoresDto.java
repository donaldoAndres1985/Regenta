package com.regenta.menu.api;

import java.math.BigDecimal;
import java.util.List;

public record CotizacionModificadoresDto(
        List<ModificadorElegidoDto> elegidos,
        BigDecimal extraPorUnidad) {
}
