package com.regenta.menu.application;

import com.regenta.menu.api.*;

import java.util.UUID;

import com.regenta.menu.domain.EstacionDeCocina;

/** Una estaciÃ³n de cocina como la ve el administrador (HU-077). */
public record EstacionDelNegocio(
        UUID id,
        String codigo,
        String nombre,
        String impresora,
        int orden,
        boolean activa) {

    static EstacionDelNegocio de(EstacionDeCocina e) {
        return new EstacionDelNegocio(e.getId(), e.getCodigo(), e.getNombre(), e.getImpresora(),
                e.getOrden(), e.isActiva());
    }
}
