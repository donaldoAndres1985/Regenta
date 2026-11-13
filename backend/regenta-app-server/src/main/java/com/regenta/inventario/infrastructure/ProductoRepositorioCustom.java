package com.regenta.inventario.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.regenta.inventario.domain.Producto;

public interface ProductoRepositorioCustom {
    List<Producto> buscarPorTokens(UUID negocioId, List<String> tokens, Pageable limite);
}
