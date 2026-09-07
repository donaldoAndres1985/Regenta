package com.regenta.inventario.aplicacion;

import java.util.UUID;

public record CategoriaDelNegocio(
        UUID id,
        UUID categoriaPadreId,
        String nombre,
        String descripcion,
        String ruta,
        int nivel,
        String icono,
        String color,
        int orden,
        boolean activa) {
}
