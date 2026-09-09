package com.regenta.menu.aplicacion;

import java.util.UUID;

import com.regenta.menu.domain.GrupoDeModificadores;

/** Un grupo de modificadores como lo ve el administrador (HU-078). */
public record GrupoDelNegocio(
        UUID id,
        String nombre,
        int minSelecciones,
        int maxSelecciones,
        boolean obligatorio,
        boolean activo) {

    static GrupoDelNegocio de(GrupoDeModificadores g) {
        return new GrupoDelNegocio(g.getId(), g.getNombre(), g.getMinSelecciones(),
                g.getMaxSelecciones(), g.esObligatorio(), g.isActivo());
    }
}
