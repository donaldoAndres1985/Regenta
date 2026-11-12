package com.regenta.ventas.infrastructure;

import java.util.List;
import java.util.UUID;

import com.regenta.ventas.domain.Venta;

public interface VentaRepositorioCustom {
    List<Venta> buscarPorTokens(UUID negocioId, List<String> tokens);
}
