package com.regenta.inventario.application;

import com.regenta.inventario.api.*;

import java.util.UUID;

/** Los filtros del buscador de inventario. {@code q} puede venir vacÃ­o (todos). */
public record FiltroDeBusqueda(
        String q,
        UUID categoriaId,
        boolean soloBajoMinimo,
        Integer limite) {
}
