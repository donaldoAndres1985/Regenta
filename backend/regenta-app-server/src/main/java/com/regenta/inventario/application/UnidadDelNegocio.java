package com.regenta.inventario.application;

import java.util.UUID;

import com.regenta.inventario.domain.UnidadMedida;

/** HU-155 · una unidad de medida tal como la ve la app. */
public record UnidadDelNegocio(UUID id, String codigo, String nombre, boolean permiteDecimales,
        boolean activa) {

    static UnidadDelNegocio de(UnidadMedida u) {
        return new UnidadDelNegocio(u.getId(), u.getCodigo(), u.getNombre(), u.isPermiteDecimales(),
                u.isActiva());
    }
}
