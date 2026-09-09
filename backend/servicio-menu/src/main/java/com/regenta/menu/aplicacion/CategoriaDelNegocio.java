package com.regenta.menu.aplicacion;

import java.util.UUID;

import com.regenta.menu.domain.CategoriaMenu;

/** Una categoría de la carta, como la ve el administrador (HU-076). */
public record CategoriaDelNegocio(
        UUID id,
        UUID cartaId,
        String nombre,
        String descripcion,
        int orden,
        String icono,
        boolean activa) {

    static CategoriaDelNegocio de(CategoriaMenu c) {
        return new CategoriaDelNegocio(c.getId(), c.getCartaId(), c.getNombre(), c.getDescripcion(),
                c.getOrden(), c.getIcono(), c.isActiva());
    }
}
