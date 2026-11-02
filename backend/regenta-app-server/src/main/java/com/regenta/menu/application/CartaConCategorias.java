package com.regenta.menu.application;

import com.regenta.menu.api.*;

import java.util.List;

/** Una carta con sus categorÃ­as en el orden definido (HU-076 criterio 2). */
public record CartaConCategorias(CartaDelNegocio carta, List<CategoriaDelNegocio> categorias) {
}
