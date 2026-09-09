package com.regenta.menu.aplicacion;

import java.util.List;

/** La carta como la ve el mesero (HU-077 criterio 4): categorías en orden, cada una con sus ítems. */
public record CartaDeMenu(CartaDelNegocio carta, List<CategoriaConItems> categorias) {

    public record CategoriaConItems(CategoriaDelNegocio categoria, List<ItemEnCarta> items) {
    }
}
