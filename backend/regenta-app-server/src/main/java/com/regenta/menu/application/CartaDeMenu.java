package com.regenta.menu.application;

import com.regenta.menu.api.*;

import java.util.List;

/** La carta como la ve el mesero (HU-077 criterio 4): categorÃ­as en orden, cada una con sus Ã­tems. */
public record CartaDeMenu(CartaDelNegocio carta, List<CategoriaConItems> categorias) {

    public record CategoriaConItems(CategoriaDelNegocio categoria, List<ItemEnCarta> items) {
    }
}
