package com.regenta.mesas.aplicacion;

import java.util.UUID;

import com.regenta.mesas.domain.Zona;

/** Una zona tal como la devuelve la API. */
public record ZonaDelNegocio(
        UUID id,
        String nombre,
        int orden,
        String color,
        boolean activa) {

    static ZonaDelNegocio de(Zona z) {
        return new ZonaDelNegocio(z.getId(), z.getNombre(), z.getOrden(), z.getColor(),
                z.isActiva());
    }
}
