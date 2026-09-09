package com.regenta.menu.aplicacion;

import java.util.List;

/** Una carta con sus categorías en el orden definido (HU-076 criterio 2). */
public record CartaConCategorias(CartaDelNegocio carta, List<CategoriaDelNegocio> categorias) {
}
