package com.regenta.inventario.aplicacion;

import java.util.UUID;

/** Los filtros del buscador de inventario. {@code q} puede venir vacío (todos). */
public record FiltroDeBusqueda(
        String q,
        UUID categoriaId,
        boolean soloBajoMinimo,
        Integer limite) {
}
