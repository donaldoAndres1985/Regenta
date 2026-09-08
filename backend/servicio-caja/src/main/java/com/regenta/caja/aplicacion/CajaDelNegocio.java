package com.regenta.caja.aplicacion;

import java.util.UUID;

import com.regenta.caja.domain.Caja;

/** Un punto de cobro del negocio. */
public record CajaDelNegocio(UUID id, String codigo, String nombre, boolean activa) {

    static CajaDelNegocio de(Caja c) {
        return new CajaDelNegocio(c.getId(), c.getCodigo(), c.getNombre(), c.isActiva());
    }
}
